package net.adoptopenjdk.api.v3.stats

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.DownloadStatsInterface
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.StatsSource
import org.slf4j.LoggerFactory
import java.time.ZoneOffset

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
        val tracking = downloadStatsInterface.getTrackingStats(10, null, null, StatsSource.all, null, null)
        tracking
                .filter { it.daily <= 0 }
                .forEach { entry ->
                    val start = entry.date.toLocalDate().atStartOfDay()
                    val end = entry.date.toLocalDate().plusDays(1).atStartOfDay()
                    LOGGER.info("Removing bad stats between $start $end")
                    runBlocking {
                        database.removeStatsBetween(start.atZone(ZoneOffset.UTC), end.atZone(ZoneOffset.UTC))
                    }
                }
    }

}