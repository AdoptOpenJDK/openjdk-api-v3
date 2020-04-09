package net.adoptopenjdk.api.v3.stats

import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.Vendor
import java.time.ZonedDateTime

class GithubDownloadStatsCalculator {
    private val database: ApiPersistence = ApiPersistenceFactory.get()

    suspend fun saveStats(repos: AdoptRepos) {

        val stats = getStats(repos)

        database.addGithubDownloadStatsEntries(stats)
    }

    public fun getStats(repos: AdoptRepos): List<GithubDownloadStatsDbEntry> {
        val date: ZonedDateTime = TimeSource.now()
        val stats = repos
                .repos
                .values
                .map { featureRelease ->
                    val total = featureRelease
                            .releases
                            .getReleases()
                            .filter { it.vendor == Vendor.adoptopenjdk }
                            .sumBy {
                                it.download_count.toInt()
                            }

                    GithubDownloadStatsDbEntry(date,
                            total.toLong(),
                            featureRelease.featureVersion)
                }
                .toList()
        return stats
    }
}
