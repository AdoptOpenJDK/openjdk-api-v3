package net.adoptopenjdk.api

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.DownloadStatsInterface
import net.adoptopenjdk.api.v3.dataSources.http.HttpClient
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.StatsSource
import net.adoptopenjdk.api.v3.stats.DockerStatsInterface
import org.junit.Assert
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class DockerStatsInterfaceTest : UpdaterTest() {

    @Test
    fun dbEntryIsCreated() {
        runBlocking {
            val client = mockk<HttpClient>()
            coEvery { client.get(any()) } answers { call ->
                if ((call.invocation.args[0] as String).contains("library")) {
                    """{"affiliation":null,"can_edit":false,"description":"x","full_description":"x","has_starred":false,"is_automated":false,"is_migrated":false,"is_private":false,"last_updated":"2020-05-13T20:14:06.911702Z","name":"adoptopenjdk","namespace":"library","permissions":{"admin":false,"read":true,"write":false},"pull_count":10625093,"repository_type":"image","star_count":147,"status":1,"user":"library"}""${'"'}"""
                } else {
                    """{"count":28,"previous":null,"results":[{"can_edit":false,"description":"x","is_automated":false,"is_migrated":false,"is_private":false,"last_updated":"2020-05-30T18:36:12.727594Z","name":"openjdk11","namespace":"adoptopenjdk","pull_count":8656446,"repository_type":"image","star_count":81,"status":1,"user":"adoptopenjdk"},{"can_edit":false,"description":"x","is_automated":false,"is_migrated":false,"is_private":false,"last_updated":"2020-05-30T17:22:21.479220Z","name":"openjdk8-openj9","namespace":"adoptopenjdk","pull_count":6760040,"repository_type":"image","star_count":36,"status":1,"user":"adoptopenjdk"}]}"""
                }
            }
            val dockerStatsInterface = DockerStatsInterface(getApiPersistence(), client)
            dockerStatsInterface.updateDb()
            val stats = getApiPersistence().getLatestAllDockerStats()
            Assert.assertTrue(stats.isNotEmpty())
        }
    }

    @Test
    fun onlyLastStatEntryPerDayIsRead() {
        runBlocking {
            val apiPersistanceMock = mockk<ApiPersistence>()
            coEvery { apiPersistanceMock.getGithubStats(any(), any<ZonedDateTime>()) } returns listOf(
                GithubDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(10), 100, mapOf(JvmImpl.hotspot to 40L, JvmImpl.openj9 to 60L), 8),
                GithubDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(20), 90, mapOf(JvmImpl.hotspot to 80L, JvmImpl.openj9 to 10L), 8),
                GithubDownloadStatsDbEntry(ZonedDateTime.now().minusDays(1), 80, mapOf(JvmImpl.hotspot to 50L, JvmImpl.openj9 to 30L), 8),
                GithubDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(15), 20, mapOf(JvmImpl.hotspot to 15L, JvmImpl.openj9 to 5L), 9)
            )

            coEvery { apiPersistanceMock.getDockerStats(any<ZonedDateTime>(), any<ZonedDateTime>()) } returns listOf(
                DockerDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(10), 100, "a-stats-repo", 8, JvmImpl.hotspot),
                DockerDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(20), 90, "a-stats-repo", 14, JvmImpl.openj9),
                DockerDownloadStatsDbEntry(ZonedDateTime.now().minusDays(1), 80, "a-stats-repo", 12, JvmImpl.openj9),
                DockerDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(15), 20, "a-different-stats-repo", 11, JvmImpl.hotspot)
            )

            val downloadStatsInterface = DownloadStatsInterface(apiPersistanceMock, getVariants())

            var stats = downloadStatsInterface.getTrackingStats(10, source = StatsSource.github, featureVersion = 8)
            assertEquals(100, stats[0].total)
            stats = downloadStatsInterface.getTrackingStats(10, source = StatsSource.github)
            assertEquals(120, stats[0].total)

            stats = downloadStatsInterface.getTrackingStats(10, source = StatsSource.dockerhub, dockerRepo = "a-stats-repo")
            assertEquals(100, stats[0].total)
            stats = downloadStatsInterface.getTrackingStats(10, source = StatsSource.dockerhub)
            assertEquals(120, stats[0].total)

            stats = downloadStatsInterface.getTrackingStats(10)
            assertEquals(240, stats[0].total)
        }
    }
}
