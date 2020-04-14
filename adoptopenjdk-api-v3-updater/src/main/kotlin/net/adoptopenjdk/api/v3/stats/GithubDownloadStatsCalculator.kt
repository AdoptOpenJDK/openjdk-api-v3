package net.adoptopenjdk.api.v3.stats

import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.JvmImpl
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

                    // Tally up jvmImpl download stats
                    val jvmImplMap : Map<JvmImpl,Long> = JvmImpl.values().map {
                        val jvmImpl = it;

                        jvmImpl to
                            featureRelease
                            .releases
                            .getReleases()
                            .filter { it.vendor == Vendor.adoptopenjdk }
                            .sumBy {
                                it.binaries
                                .filter { it.jvm_impl == jvmImpl }
                                .sumBy {
                                    it.download_count.toInt()
                                }
                            }
                            .toLong()
                    }.toMap()

                    GithubDownloadStatsDbEntry(date,
                            total.toLong(),
                            jvmImplMap,
                            featureRelease.featureVersion)
                }
                .toList()
        return stats
    }
}