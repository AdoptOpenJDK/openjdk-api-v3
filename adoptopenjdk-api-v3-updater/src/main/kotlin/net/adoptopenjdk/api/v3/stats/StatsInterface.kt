package net.adoptopenjdk.api.v3.stats

import java.time.ZoneOffset
import java.time.ZonedDateTime
import net.adoptopenjdk.api.v3.DownloadStatsInterface
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.StatsSource
import org.slf4j.LoggerFactory

class StatsInterface {

    private val githubDownloadStatsCalculator: GithubDownloadStatsCalculator = GithubDownloadStatsCalculator()
    private val dockerStatsInterface: DockerStatsInterface = DockerStatsInterface()
    private var database: ApiPersistence = ApiPersistenceFactory.get()

    private val downloadStatsInterface = DownloadStatsInterface()

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    suspend fun update(repos: AdoptRepos) {
        githubDownloadStatsCalculator.saveStats(repos)
        dockerStatsInterface.updateDb()

        removeBadDownloadStats()
    }

    private suspend fun removeBadDownloadStats() {
        val tracking = downloadStatsInterface.getTrackingStats(days = 10, source = StatsSource.all)
        tracking
                .filter { it.daily <= 0 }
                .forEach { entry ->
                    val start = entry.date.toLocalDate().atStartOfDay().atZone(ZoneOffset.UTC)
                    val end = entry.date.toLocalDate().plusDays(1).atStartOfDay().atZone(ZoneOffset.UTC)

                    printStatDebugInfo(start, end)
                    database.removeStatsBetween(start, end)
                }
    }

    private suspend fun printStatDebugInfo(start: ZonedDateTime, end: ZonedDateTime) {
        LOGGER.info("Removing bad stats between $start $end")
        printStats(start, end)
        LOGGER.info("Day before: $start $end")
        printStats(start.minusDays(1), end.minusDays(1))
    }

    private suspend fun printStats(start: ZonedDateTime, end: ZonedDateTime) {
        database
                .getGithubStats(start, end)
                .forEach { stat ->
                    LOGGER.info("github stat: ${stat.feature_version} ${stat.downloads}")
                }

        database
                .getDockerStats(start, end)
                .forEach { stat ->
                    LOGGER.info("docker stat: ${stat.repo} ${stat.pulls}")
                }
    }
}
