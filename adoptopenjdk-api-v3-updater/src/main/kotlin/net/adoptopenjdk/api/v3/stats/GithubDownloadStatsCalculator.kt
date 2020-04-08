package net.adoptopenjdk.api.v3.stats

import java.time.ZonedDateTime
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.Vendor

class GithubDownloadStatsCalculator {
    private val database: ApiPersistence = ApiPersistenceFactory.get()

    suspend fun saveStats(repos: AdoptRepos) {
        val date: ZonedDateTime = TimeSource.now()

        val stats = repos
                .repos
                .values
                .map { featureRelease ->
                    val total = featureRelease
                            .releases
                            .getReleases()
                            .filter { it.vendor == Vendor.adoptopenjdk }
                            .sumBy { it.download_count.toInt() }

                    GithubDownloadStatsDbEntry(date,
                            total.toLong(),
                            featureRelease.featureVersion)
                }
                .toList()

        database.addGithubDownloadStatsEntries(stats)
    }
}
