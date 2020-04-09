package net.adoptopenjdk.api

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.DownloadStatsInterface
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.DefaultUpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.StatsSource
import net.adoptopenjdk.api.v3.stats.DockerStatsInterface
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class DockerStatsInterfaceTest {
    companion object {
        @JvmStatic
        @BeforeAll
        @Override
        fun startDb() {
            UpdaterHtmlClientFactory.client = DefaultUpdaterHtmlClient()
            BaseTest.startFongo()
        }
    }

    @Test
    fun dbEntryIsCreated() {
        runBlocking {
            DockerStatsInterface().updateDb()

            val stats = ApiPersistenceFactory.get().getLatestAllDockerStats()
            Assert.assertTrue(stats.size > 0)
        }
    }

    @Test
    fun onlyLastStatEntryPerDayIsRead() {
        runBlocking {
            val apiPersistanceMock = mockk<ApiPersistence>()
            coEvery { apiPersistanceMock.getGithubStats(any<ZonedDateTime>(), any<ZonedDateTime>()) } returns listOf(
                    GithubDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(10), 100, 8),
                    GithubDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(20), 90, 8),
                    GithubDownloadStatsDbEntry(ZonedDateTime.now().minusDays(1), 80, 8),
                    GithubDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(15), 20, 9)
            )

            coEvery { apiPersistanceMock.getDockerStats(any<ZonedDateTime>(), any<ZonedDateTime>()) } returns listOf(
                    DockerDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(10), 100, "a-stats-repo"),
                    DockerDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(20), 90, "a-stats-repo"),
                    DockerDownloadStatsDbEntry(ZonedDateTime.now().minusDays(1), 80, "a-stats-repo"),
                    DockerDownloadStatsDbEntry(ZonedDateTime.now().minusMinutes(15), 20, "a-different-stats-repo")
            )

            val downloadStatsInterface = DownloadStatsInterface(apiPersistanceMock)

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