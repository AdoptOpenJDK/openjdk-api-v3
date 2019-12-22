package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoApiPersistence.Companion.DOCKER_STATS_DB
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoApiPersistence.Companion.GITHUB_STATS_DB
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoClientFactory
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.DownloadStats
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime


@QuarkusTest
class DownloadStatsPathTest : BaseTest() {

    companion object {
        @JvmStatic
        @BeforeAll
        fun before() {
            populateDb()
        }
    }

    @Test
    fun whenEmptyResultIsSane() {
        runBlocking {
            MongoClientFactory.get().database.dropCollection(GITHUB_STATS_DB)
            MongoClientFactory.get().database.dropCollection(DOCKER_STATS_DB)
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, DownloadStats::class.java)
                            return stats.total_downloads.total == 0L
                        }
                    })
        }
    }

    @Test
    fun totalDownloadReturnsSaneData() {
        runBlocking {
            mockStats()
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, DownloadStats::class.java)
                            return stats.total_downloads.total == 120L &&
                                    stats.total_downloads.docker_pulls == 70L &&
                                    stats.total_downloads.github_downloads == 50L &&
                                    stats.github_downloads[8] == 30L &&
                                    stats.github_downloads[9] == 20L &&
                                    stats.docker_pulls["a-repo-name"] == 40L &&
                                    stats.docker_pulls["b-repo-name"] == 30L
                        }
                    })
        }
    }


    @Test
    fun totalVersionReturnsSaneData() {
        runBlocking {
            mockStats()
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total/8")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, Map::class.java)
                            return stats.isNotEmpty() && !stats.containsValue(0L)
                        }
                    })
        }
    }

    @Test
    fun totalTagReturnsSaneData() {
        runBlocking {
            mockStats()
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total/8/jdk8u232-b09")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, Map::class.java)
                            return stats.isNotEmpty() && !stats.containsValue(0L)
                        }
                    })
        }
    }

    @Test
    fun trackingReturnsSaneData() {
        runBlocking {
            mockStats()
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/tracking")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, List::class.java)
                            return stats.isNotEmpty()
                        }
                    })
        }
    }

    private suspend fun mockStats() {
        val persistance = ApiPersistenceFactory.get()

        persistance.addDockerDownloadStatsEntries(
                createDockerStatsWithRepoName()
        )

        persistance.addGithubDownloadStatsEntries(
                createGithubData()
        )
    }

    private fun createGithubData(): List<GithubDownloadStatsDbEntry> {
        return listOf(
                GithubDownloadStatsDbEntry(
                        LocalDateTime.now().minusDays(10),
                        10,
                        8
                ),
                GithubDownloadStatsDbEntry(
                        LocalDateTime.now().minusDays(5),
                        20,
                        9
                ),
                GithubDownloadStatsDbEntry(
                        LocalDateTime.now().minusDays(1),
                        30,
                        8
                )
        )
    }

    private fun createDockerStatsWithRepoName(): List<DockerDownloadStatsDbEntry> {
        return listOf(
                DockerDownloadStatsDbEntry(
                        LocalDateTime.now().minusDays(10),
                        20,
                        "a-repo-name"
                ),
                DockerDownloadStatsDbEntry(
                        LocalDateTime.now().minusDays(5),
                        30,
                        "b-repo-name"
                ),
                DockerDownloadStatsDbEntry(
                        LocalDateTime.now().minusDays(1),
                        40,
                        "a-repo-name"
                )
        )
    }


}

