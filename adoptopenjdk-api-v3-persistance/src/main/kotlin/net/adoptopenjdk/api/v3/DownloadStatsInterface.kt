package net.adoptopenjdk.api.v3

import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.DbStatsEntry
import net.adoptopenjdk.api.v3.models.DownloadDiff
import net.adoptopenjdk.api.v3.models.MonthlyDownloadDiff
import net.adoptopenjdk.api.v3.models.DownloadStats
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.StatsSource
import net.adoptopenjdk.api.v3.models.TotalStats
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

class StatEntry(
    val dateTime: ZonedDateTime,
    val count: Long
)

class DownloadStatsInterface(
    private val dataStore: ApiPersistence = ApiPersistenceFactory.get()
) {

    suspend fun getTrackingStats(
        days: Int? = null,
        from: ZonedDateTime? = null,
        to: ZonedDateTime? = null,
        source: StatsSource? = null,
        featureVersion: Int? = null,
        dockerRepo: String? = null,
        jvmImpl: JvmImpl? = null
    ): List<DownloadDiff> {

        // need +1 as for a diff you need num days +1 from db
        val daysSince = (days ?: 30) + 1
        val statsSource = source ?: StatsSource.all
        val periodEnd = to ?: TimeSource.now()

        // Cap maximum period to 180 days
        val periodMinusDays = periodEnd.minusDays(min(180, daysSince).toLong())

        val periodStart = if (from != null) {
            if (periodMinusDays.isAfter(from)) {
                periodMinusDays
            } else {
                from
            }
        } else {
            periodMinusDays
        }

        // look for stats up to 10 days before the requested start, this is because for a daily diff we need the
        // previous entry before the period started
        val stats = getStats(periodStart.minusDays(10), periodEnd, featureVersion, dockerRepo, jvmImpl, statsSource)

        return calculateDailyDiff(stats, periodStart, periodEnd, days)
    }

    suspend fun getMonthlyTrackingStats(
        to: ZonedDateTime? = null,
        source: StatsSource? = null,
        featureVersion: Int? = null,
        dockerRepo: String? = null,
        jvmImpl: JvmImpl? = null
    ): List<MonthlyDownloadDiff> {

        val periodEnd = to ?: TimeSource.now().withDayOfMonth(1)
        val periodStart = periodEnd.minusMonths(6).withDayOfMonth(1)
        val statsSource = source ?: StatsSource.all

        val stats = getMonthlyStats(periodStart.minusDays(10), periodEnd.minusDays(1), featureVersion, dockerRepo, jvmImpl, statsSource)
        return calculateMonthlyDiff(stats)
    }

    private suspend fun getStats(
        start: ZonedDateTime,
        end: ZonedDateTime,
        featureVersion: Int?,
        dockerRepo: String?,
        jvmImpl: JvmImpl?,
        statsSource: StatsSource
    ): Collection<StatEntry> {
        val githubGrouped = getGithubDownloadStatsByDate(start, end, featureVersion, jvmImpl)
        val dockerGrouped = getDockerDownloadStatsByDate(start, end, featureVersion, dockerRepo, jvmImpl)

        val stats = when (statsSource) {
            StatsSource.dockerhub -> dockerGrouped
            StatsSource.github -> githubGrouped
            else -> githubGrouped.union(dockerGrouped)
        }

        return stats.groupBy { it.dateTime.toLocalDate() }
            .map { grouped ->
                StatEntry(
                        grouped.value.map { it.dateTime }.max()!!,
                        grouped.value.map { it.count }.sum()
                )
            }
            .sortedBy { it.dateTime }
    }

    private suspend fun getMonthlyStats(
        start: ZonedDateTime,
        end: ZonedDateTime,
        featureVersion: Int?,
        dockerRepo: String?,
        jvmImpl: JvmImpl?,
        statsSource: StatsSource
    ): Collection<StatEntry> {
        return getStats(start, end, featureVersion, dockerRepo, jvmImpl, statsSource)
            .groupBy { it.dateTime.getMonth() }
            .map { grouped ->
                    grouped.value.maxBy { it.dateTime }!!
            }
    }

    private fun calculateDailyDiff(
        stats: Collection<StatEntry>,
        periodStart: ZonedDateTime,
        periodEnd: ZonedDateTime,
        days: Int?
    ): List<DownloadDiff> {
        return stats
            .windowed(2, 1, false) {
                val minDiff = max(1, ChronoUnit.MINUTES.between(it[0].dateTime, it[1].dateTime))
                val downloadDiff = ((it[1].count - it[0].count) * 60L * 24L) / minDiff
                DownloadDiff(it[1].dateTime, it[1].count, downloadDiff)
            }
            .filter { it.date.isAfter(periodStart) && it.date.isBefore(periodEnd) }
            .takeLast(days ?: Int.MAX_VALUE)
    }

    private fun calculateMonthlyDiff(
        stats: Collection<StatEntry>
    ): List<MonthlyDownloadDiff> {
        val toTwoChar = { value: Int -> if (value < 10) "0" + value else value.toString() } // Returns in MM format

        return stats
            .windowed(2, 1, false) {
                MonthlyDownloadDiff(
                    it[1].dateTime.getYear().toString() + "-" + toTwoChar(it[1].dateTime.getMonthValue()),
                    it[1].count,
                    it[1].count - it[0].count
                )
            }
    }

    private suspend fun getGithubDownloadStatsByDate(start: ZonedDateTime, end: ZonedDateTime, featureVersion: Int?, jvmImpl: JvmImpl?): List<StatEntry> {
        return sumDailyStats(
                dataStore
                        .getGithubStats(start, end)
                        .groupBy { it.date.toLocalDate() }
                        .flatMap { grouped ->
                            grouped.value
                                    .groupBy { it.feature_version }
                                    .map { featureVersionsForDay ->
                                        featureVersionsForDay.value.maxBy { it.date }!!
                                    }
                        }
                        .filter {
                            (featureVersion == null || it.feature_version == featureVersion) &&
                            (jvmImpl == null || it.jvmImplDownloads != null)
                        }
                        .sortedBy { it.date },
                jvmImpl
        )
    }

    private suspend fun getDockerDownloadStatsByDate(start: ZonedDateTime, end: ZonedDateTime, featureVersion: Int?, dockerRepo: String?, jvmImpl: JvmImpl?): List<StatEntry> {
        return sumDailyStats(
                dataStore
                        .getDockerStats(start, end)
                        .groupBy { it.date.toLocalDate() }
                        .flatMap { grouped ->
                            grouped.value
                                    .groupBy { it.repo }
                                    .map { repoForDay ->
                                        repoForDay.value.maxBy { it.date }!!
                                    }
                        }
                        .filter {
                            (dockerRepo == null || it.repo == dockerRepo) &&
                            (featureVersion == null || it.feature_version == featureVersion) &&
                            (jvmImpl == null || it.jvm_impl == jvmImpl)
                        }
                        .sortedBy { it.date }
        )
    }

    private fun <T> sumDailyStats(stats: List<DbStatsEntry<T>>): List<StatEntry> {
        return stats
                .groupBy { it.date.toLocalDate() }
                .map { grouped -> StatEntry(getLastDate(grouped.value), formTotalDownloads(grouped.value)) }
    }

    private fun sumDailyStats(githubStats: List<GithubDownloadStatsDbEntry>, jvmImpl: JvmImpl?): List<StatEntry> {
        jvmImpl ?: return sumDailyStats(githubStats)

        return githubStats
                .groupBy { it.date.toLocalDate() }
                .map { grouped -> StatEntry(getLastDate(grouped.value), formTotalDownloads(grouped.value, jvmImpl)) }
    }

    private fun <T> getLastDate(grouped: List<DbStatsEntry<T>>): ZonedDateTime {
        return grouped
                .maxBy { it.date }!!
                .date
    }

    private fun <T> formTotalDownloads(stats: List<DbStatsEntry<T>>): Long {
        return stats
                .groupBy { it.getId() }
                .map { grouped -> grouped.value.maxBy { it.date } }
                .map { it!!.getMetric() }
                .sum()
    }

    private fun formTotalDownloads(stats: List<GithubDownloadStatsDbEntry>, jvmImpl: JvmImpl): Long {
        return stats
                .groupBy { it.getId() }
                .map { grouped -> grouped.value.maxBy { it.date } }
                .map { (it!!.jvmImplDownloads?.get(jvmImpl) ?: 0) }
                .sum()
    }

    suspend fun getTotalDownloadStats(): DownloadStats {
        val dockerStats = dataStore.getLatestAllDockerStats()

        val githubStats = getGithubStats()

        val dockerPulls = dockerStats
                .map { it.pulls }
                .sum()

        val githubDownloads = githubStats
                .map { it.downloads }
                .sum()

        val dockerBreakdown = dockerStats
                .map { Pair(it.repo, it.pulls) }
                .toMap()

        val githubBreakdown = githubStats
                .map { Pair(it.feature_version, it.downloads) }
                .toMap()

        val totalStats = TotalStats(dockerPulls, githubDownloads, dockerPulls + githubDownloads)
        return DownloadStats(TimeSource.now(), totalStats, githubBreakdown, dockerBreakdown)
    }

    private suspend fun getGithubStats(): List<GithubDownloadStatsDbEntry> {
        return APIDataStore.variants.versions
                .mapNotNull { featureVersion ->
                    dataStore.getLatestGithubStatsForFeatureVersion(featureVersion)
                }
    }
}
