package net.adoptopenjdk.api.v3.stats

import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import java.time.LocalDateTime

class GithubDownloadStatsCalculator {
    private val database: ApiPersistence = ApiPersistenceFactory.get()

    suspend fun saveStats(repos: AdoptRepos) {
        val date: LocalDateTime = LocalDateTime.now()

        val stats = repos
                .repos
                .values
                .map { featureRelease ->
                    val total = featureRelease
                            .releases
                            .getReleases()
                            .sumBy { it.download_count.toInt() }

                    GithubDownloadStatsDbEntry(date,
                            total.toLong(),
                            featureRelease.featureVersion)
                }
                .toList()

        database.addGithubDownloadStatsEntries(stats)
    }
}