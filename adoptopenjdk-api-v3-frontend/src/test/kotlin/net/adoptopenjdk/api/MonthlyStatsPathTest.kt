package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.GitHubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.JvmImpl
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(value = [DbExtension::class])
@QuarkusTest
class MonthlyStatsPathTest : FrontendTest() {

    @BeforeEach
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

    private fun createGithubData(): List<GitHubDownloadStatsDbEntry> {
        return listOf(
            GitHubDownloadStatsDbEntry(
                TimeSource.now(),
                800,
                mapOf(JvmImpl.hotspot to 600L, JvmImpl.openj9 to 200L),
                11
            ),
            GitHubDownloadStatsDbEntry(
                TimeSource.now().minusMonths(1).withDayOfMonth(26),
                600,
                mapOf(JvmImpl.hotspot to 450L, JvmImpl.openj9 to 150L),
                8
            ),
            GitHubDownloadStatsDbEntry(
                TimeSource.now().minusMonths(1).withDayOfMonth(4),
                400,
                mapOf(JvmImpl.hotspot to 300L, JvmImpl.openj9 to 100L),
                11
            ),
            GitHubDownloadStatsDbEntry(
                TimeSource.now().minusMonths(2).withDayOfMonth(23),
                350,
                mapOf(JvmImpl.hotspot to 275L, JvmImpl.openj9 to 75L),
                11
            ),
            GitHubDownloadStatsDbEntry(
                TimeSource.now().minusMonths(3).withDayOfMonth(24),
                200,
                mapOf(JvmImpl.hotspot to 150L, JvmImpl.openj9 to 50L),
                8
            ),
            GitHubDownloadStatsDbEntry(
                TimeSource.now().minusMonths(4).withDayOfMonth(19),
                100,
                null,
                8
            ),
            GitHubDownloadStatsDbEntry(
                TimeSource.now().minusMonths(4).withDayOfMonth(19),
                100,
                null,
                11
            )
        )
    }

    private fun createDockerStatsWithRepoName(): List<DockerDownloadStatsDbEntry> {
        return listOf(
            DockerDownloadStatsDbEntry(
                TimeSource.now(),
                600,
                "a-repo-name",
                8,
                JvmImpl.hotspot
            ),
            DockerDownloadStatsDbEntry(
                TimeSource.now().minusMonths(1).withDayOfMonth(26),
                500,
                "b-repo-name",
                8,
                JvmImpl.openj9
            ),
            DockerDownloadStatsDbEntry(
                TimeSource.now().minusMonths(1).withDayOfMonth(4),
                310,
                "a-repo-name",
                11,
                JvmImpl.openj9
            ),
            DockerDownloadStatsDbEntry(
                TimeSource.now().minusMonths(2).withDayOfMonth(23),
                230,
                "a-repo-name",
                11,
                JvmImpl.hotspot
            ),
            DockerDownloadStatsDbEntry(
                TimeSource.now().minusMonths(3).withDayOfMonth(24),
                150,
                "b-repo-name",
                8,
                JvmImpl.hotspot
            ),
            DockerDownloadStatsDbEntry(
                TimeSource.now().minusMonths(4).withDayOfMonth(19),
                50,
                "a-repo-name",
                null,
                null
            ),
            DockerDownloadStatsDbEntry(
                TimeSource.now().minusMonths(4).withDayOfMonth(19),
                50,
                "b-repo-name",
                null,
                null
            )
        )
    }

    @Test
    fun trackingReturnsSaneData() {
        runBlocking {
            RestAssured.given()
                .`when`()
                .get("/v3/stats/downloads/monthly")
                .then()
                .body(object : TypeSafeMatcher<String>() {

                    override fun describeTo(description: Description?) {
                        description!!.appendText("json")
                    }

                    override fun matchesSafely(p0: String?): Boolean {
                        val stats = JsonMapper.mapper.readValue(p0, List::class.java)
                        return stats.size == 3 &&
                            (stats[0] as Map<String, *>).get("total") == 350 &&
                            (stats[0] as Map<String, *>).get("monthly") == 50 &&
                            (stats[1] as Map<String, *>).get("total") == 580 &&
                            (stats[1] as Map<String, *>).get("monthly") == 230 &&
                            (stats[2] as Map<String, *>).get("total") == 1100 &&
                            (stats[2] as Map<String, *>).get("monthly") == 520
                    }
                })
        }
    }

    @Test
    fun trackingFeatureVersionRetrunsSaneData() {
        runBlocking {
            RestAssured.given()
                .`when`()
                .get("/v3/stats/downloads/monthly?feature_version=8")
                .then()
                .body(object : TypeSafeMatcher<String>() {

                    override fun describeTo(description: Description?) {
                        description!!.appendText("json")
                    }

                    override fun matchesSafely(p0: String?): Boolean {
                        val stats = JsonMapper.mapper.readValue(p0, List::class.java)
                        return stats.size == 2 &&
                            (stats[0] as Map<String, *>).get("total") == 350 &&
                            (stats[0] as Map<String, *>).get("monthly") == 250 &&
                            (stats[1] as Map<String, *>).get("total") == 1100 &&
                            (stats[1] as Map<String, *>).get("monthly") == 750
                    }
                })
        }
    }

    @Test
    fun trackingJvmImplRetrunsSaneData() {
        runBlocking {
            RestAssured.given()
                .`when`()
                .get("/v3/stats/downloads/monthly?jvm_impl=hotspot")
                .then()
                .body(object : TypeSafeMatcher<String>() {

                    override fun describeTo(description: Description?) {
                        description!!.appendText("json")
                    }

                    override fun matchesSafely(p0: String?): Boolean {
                        val stats = JsonMapper.mapper.readValue(p0, List::class.java)
                        return stats.size == 2 &&
                            (stats[0] as Map<String, *>).get("total") == 505 &&
                            (stats[0] as Map<String, *>).get("monthly") == 205 &&
                            (stats[1] as Map<String, *>).get("total") == 450 &&
                            (stats[1] as Map<String, *>).get("monthly") == -55
                    }
                })
        }
    }

    @Test
    fun trackingDockerRepoRetrunsSaneData() {
        runBlocking {
            RestAssured.given()
                .`when`()
                .get("/v3/stats/downloads/monthly?source=dockerhub&docker_repo=a-repo-name")
                .then()
                .body(object : TypeSafeMatcher<String>() {

                    override fun describeTo(description: Description?) {
                        description!!.appendText("json")
                    }

                    override fun matchesSafely(p0: String?): Boolean {
                        val stats = JsonMapper.mapper.readValue(p0, List::class.java)
                        return stats.size == 2 &&
                            (stats[0] as Map<String, *>).get("total") == 230 &&
                            (stats[0] as Map<String, *>).get("monthly") == 180 &&
                            (stats[1] as Map<String, *>).get("total") == 310 &&
                            (stats[1] as Map<String, *>).get("monthly") == 80
                    }
                })
        }
    }
}
