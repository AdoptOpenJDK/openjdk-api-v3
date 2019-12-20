package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.DownloadStats
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.jupiter.api.Test
import java.time.LocalDateTime


@QuarkusTest
class DownloadStatsPathTest : BaseTest() {


    @Test
    fun whenEmptyResultIsSane() {

        runBlocking {
            val persistance = ApiPersistenceFactory.get()

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
            val persistance = ApiPersistenceFactory.get()

            persistance.addDockerDownloadStatsEntries(
                    createDockerStatsWithRepoName()
            )

            persistance.addGithubDownloadStatsEntries(
                    createGithubData()
            )


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

