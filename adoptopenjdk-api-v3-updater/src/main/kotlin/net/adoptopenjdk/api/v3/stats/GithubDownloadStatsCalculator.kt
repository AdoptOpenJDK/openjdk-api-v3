package net.adoptopenjdk.api.v3.stats

import javax.inject.Inject
import javax.inject.Singleton
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.Vendor
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

@Singleton
class GithubDownloadStatsCalculator @Inject constructor(private val database: ApiPersistence) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    suspend fun saveStats(repos: AdoptRepos) {

        val stats = getStats(repos)

        database.addGithubDownloadStatsEntries(stats)

        printSizeStats(repos)
    }

    fun printSizeStats(repos: AdoptRepos) {
        val stats = repos
            .repos
            .values
            .map { featureRelease ->
                val total = featureRelease
                    .releases
                    .getReleases()
                    .filter { it.vendor == Vendor.adoptopenjdk }
                    .flatMap { release ->
                        release
                            .binaries
                            .map { it.`package`.size + if (it.installer == null) 0 else it.installer!!.size }
                            .asSequence()
                    }
                    .sum()

                LOGGER.info("Stats ${featureRelease.featureVersion} $total")
                total
            }
            .sum()
        LOGGER.info("Stats total $stats")
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
                val jvmImplMap: Map<JvmImpl, Long> = JvmImpl.values().map { jvmImpl ->
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
                    featureRelease.featureVersion
                )
            }
            .toList()
        return stats
    }
}
