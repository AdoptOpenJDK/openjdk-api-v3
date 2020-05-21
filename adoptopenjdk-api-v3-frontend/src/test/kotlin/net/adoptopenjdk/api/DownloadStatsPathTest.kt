package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.DownloadStats
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.JvmImpl
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter
import kotlin.test.assertFails

@QuarkusTest
class DownloadStatsPathTest : BaseTest() {
    companion object {
        @JvmStatic
        @BeforeAll
        fun before() {
            populateDb()
            mockStats()
        }

        fun mockStats() {
            runBlocking {
                val persistance = ApiPersistenceFactory.get()

                persistance.addDockerDownloadStatsEntries(
                        createDockerStatsWithRepoName()
                )

                persistance.addGithubDownloadStatsEntries(
                        createGithubData()
                )
            }
        }

        private fun createGithubData(): List<GithubDownloadStatsDbEntry> {
            return listOf(
                    GithubDownloadStatsDbEntry(
                            TimeSource.now().minusDays(15),
                            0,
                            null,
                            11
                    ),
                    GithubDownloadStatsDbEntry(
                            TimeSource.now().minusDays(10),
                            10,
                            mapOf(JvmImpl.hotspot to 10L),
                            11
                    ),
                    GithubDownloadStatsDbEntry(
                            TimeSource.now().minusDays(5),
                            20,
                            mapOf(JvmImpl.hotspot to 16L, JvmImpl.openj9 to 4L),
                            8
                    ),
                    GithubDownloadStatsDbEntry(
                            TimeSource.now().minusDays(1),
                            40,
                            mapOf(JvmImpl.hotspot to 30L, JvmImpl.openj9 to 10L),
                            11
                    ),
                    GithubDownloadStatsDbEntry(
                            TimeSource.now().minusDays(1).minusMinutes(1),
                            25,
                            mapOf(JvmImpl.hotspot to 20L, JvmImpl.openj9 to 5L),
                            8
                    ),
                    GithubDownloadStatsDbEntry(
                            TimeSource.now().minusDays(1),
                            30,
                            mapOf(JvmImpl.hotspot to 20L, JvmImpl.openj9 to 10L),
                            8
                    )
            )
        }

        private fun createDockerStatsWithRepoName(): List<DockerDownloadStatsDbEntry> {
            return listOf(
                    DockerDownloadStatsDbEntry(
                            TimeSource.now().minusDays(15),
                            0,
                            "b-repo-name",
                            null,
                            null
                    ),
                    DockerDownloadStatsDbEntry(
                            TimeSource.now().minusDays(10),
                            20,
                            "a-repo-name",
                            11,
                            JvmImpl.openj9
                    ),
                    DockerDownloadStatsDbEntry(
                            TimeSource.now().minusDays(5),
                            30,
                            "b-repo-name",
                            8,
                            JvmImpl.hotspot
                    ),
                    DockerDownloadStatsDbEntry(
                            TimeSource.now().minusDays(1),
                            40,
                            "b-repo-name",
                            11,
                            JvmImpl.hotspot
                    ),
                    DockerDownloadStatsDbEntry(
                            TimeSource.now().minusDays(1).minusMinutes(1),
                            50,
                            "a-repo-name",
                            8,
                            JvmImpl.openj9
                    ),
                    DockerDownloadStatsDbEntry(
                            TimeSource.now().minusDays(1),
                            60,
                            "a-repo-name",
                            8,
                            JvmImpl.openj9
                    )
            )
        }
    }

    @Test
    fun totalDownloadReturnsSaneData() {
        runBlocking {
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
                            return stats.total_downloads.total == 170L &&
                                    stats.total_downloads.docker_pulls == 100L &&
                                    stats.total_downloads.github_downloads == 70L &&
                                    stats.github_downloads[8] == 30L &&
                                    stats.github_downloads[11] == 40L &&
                                    stats.docker_pulls["a-repo-name"] == 60L &&
                                    stats.docker_pulls["b-repo-name"] == 40L
                        }
                    })
        }
    }

    @Test
    fun totalVersionReturnsSaneData() {
        runBlocking {
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
    fun badTotalVersionReturnsSaneData() {
        runBlocking {
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total/101")
                    .then()
                    .statusCode(400)
        }
    }

    @Test
    fun totalTagReturnsSaneData() {
        runBlocking {
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
    fun badTotalTagReturnsSaneData() {
        runBlocking {
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total/101/fooBar")
                    .then()
                    .statusCode(400)
        }
    }

    @Test
    fun trackingReturnsSaneData() {
        runBlocking {
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
                            return stats.size == 3 &&
                                    (stats[0] as Map<String, *>).get("total") == 30 &&
                                    (stats[0] as Map<String, *>).get("daily") == 6 &&
                                    (stats[1] as Map<String, *>).get("total") == 50 &&
                                    (stats[1] as Map<String, *>).get("daily") == 4 &&
                                    (stats[2] as Map<String, *>).get("total") == 170 &&
                                    (stats[2] as Map<String, *>).get("daily") == 30
                        }
                    })
        }
    }

    @Test
    fun trackingFeatureVersionRetrunsSaneData() {
        runBlocking {
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/tracking?feature_version=11")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, List::class.java)
                            return stats.size == 2 &&
                                    (stats[0] as Map<String, *>).get("total") == 30 &&
                                    (stats[0] as Map<String, *>).get("daily") == 6 &&
                                    (stats[1] as Map<String, *>).get("total") == 80 &&
                                    (stats[1] as Map<String, *>).get("daily") == 5
                        }
                    })
        }
    }

    @Test
    fun trackingJvmImplRetrunsSaneData() {
        runBlocking {
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/tracking?jvm_impl=hotspot")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, List::class.java)
                            return stats.size == 2 &&
                                    (stats[0] as Map<String, *>).get("total") == 46 &&
                                    (stats[0] as Map<String, *>).get("daily") == 7 &&
                                    (stats[1] as Map<String, *>).get("total") == 90 &&
                                    (stats[1] as Map<String, *>).get("daily") == 11
                        }
                    })
        }
    }

    @Test
    fun trackingDockerRepoRetrunsSaneData() {
        runBlocking {
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/tracking?source=dockerhub&docker_repo=a-repo-name")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, List::class.java)
                            return stats.size == 1 &&
                                    (stats[0] as Map<String, *>).get("total") == 60 &&
                                    (stats[0] as Map<String, *>).get("daily") == 4
                        }
                    })
        }
    }

    @Test
    fun dateRangeFilterWithStartAndEndIsCorrect() {
        requestStats(
                TimeSource.date().minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE),
                TimeSource.date().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE),
                null,
                { stats ->
                    stats.size == 1 &&
                            (stats[0] as Map<String, *>).get("total") == 50 &&
                            (stats[0] as Map<String, *>).get("daily") == 4
                })
    }

    @Test
    fun dateRangeFilterWithEndIsCorrect() {
        requestStats(
                null,
                TimeSource.date().minusDays(2).format(DateTimeFormatter.ISO_DATE),
                null,
                { stats ->
                    stats.size == 2 &&
                            (stats[0] as Map<String, *>).get("total") == 30 &&
                            (stats[0] as Map<String, *>).get("daily") == 6 &&
                            (stats[1] as Map<String, *>).get("total") == 50 &&
                            (stats[1] as Map<String, *>).get("daily") == 4
                })
    }

    @Test
    fun dateRangeFilterWithEndAndDayIsCorrect() {
        requestStats(
                null,
                TimeSource.date().format(DateTimeFormatter.ISO_DATE),
                1,
                { stats ->
                    stats.size == 1 &&
                            (stats[0] as Map<String, *>).get("total") == 170 &&
                            (stats[0] as Map<String, *>).get("daily") == 30
                })
    }

    @Test
    fun throwsOnABadDate() {
        assertFails({
            requestStats(
                    null,
                    "foo",
                    1,
                    { stats ->
                        stats.size == 1 &&
                                (stats[0] as Map<String, *>).get("total") == 170 &&
                                (stats[0] as Map<String, *>).get("daily") == 30
                    })
        })
    }

    private fun requestStats(from: String?, to: String?, days: Int?, check: (List<*>) -> Boolean): ValidatableResponse? {
        return runBlocking {

            var url = "/v3/stats/downloads/tracking?"
            if (from != null) {
                url += "from=$from&"
            }
            if (to != null) {
                url += "to=$to&"
            }
            if (days != null) {
                url += "days=$days&"
            }

            RestAssured.given()
                    .`when`()
                    .get(url)
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            return check(JsonMapper.mapper.readValue(p0, List::class.java))
                        }
                    })
        }
    }
}
