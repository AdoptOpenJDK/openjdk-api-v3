package net.adoptopenjdk.api.v3.routes.stats

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.models.DownloadDiff
import net.adoptopenjdk.api.v3.models.DownloadStats
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.TotalStats
import net.adoptopenjdk.api.v3.models.Vendor
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.jboss.resteasy.annotations.jaxrs.PathParam
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import javax.ws.rs.BadRequestException
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import kotlin.math.max
import kotlin.math.min


@Path("/stats/downloads")
@Produces(MediaType.APPLICATION_JSON)
@Schema(hidden = true)
class DownloadStatsResource {
    private val dataStore = ApiPersistenceFactory.get()

    @GET
    @Path("/total")
    @Operation(summary = "Get download stats", description = "stats", hidden = true)
    @Schema(hidden = true)
    fun getTotalDownloadStats(): CompletionStage<DownloadStats> {
        return runAsync {
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
            return@runAsync DownloadStats(LocalDateTime.now(), totalStats, githubBreakdown, dockerBreakdown)
        }
    }

    private suspend fun getGithubStats(): List<GithubDownloadStatsDbEntry> {
        return APIDataStore.variants.versions
                .mapNotNull { featureVersion ->
                    dataStore.getLatestGithubStatsForFeatureVersion(featureVersion)
                }
    }

    @GET
    @Path("/total/{feature_version}")
    @Operation(summary = "Get download stats for feature verson", description = "stats", hidden = true)
    @Schema(hidden = true)
    fun getTotalDownloadStats(
            @Parameter(name = "feature_version", description = "Feature version (i.e 8, 9, 10...)", required = true)
            @PathParam("feature_version")
            featureVersion: Int
    ): Map<String, Long> {
        val release = APIDataStore.getAdoptRepos().getFeatureRelease(featureVersion)
                ?: throw BadRequestException("Unable to find version $featureVersion")

        return getAdoptReleases(release)
                .filter { it.release_type == ReleaseType.ga }
                .groupBy { it.version_data }
                .map { grouped -> Pair(grouped.key.semver, grouped.value.map { it.download_count }.sum()) }
                .toMap()
    }


    @Throws(BadRequestException::class)
    @GET
    @Path("/total/{feature_version}/{release_name}")
    @Operation(summary = "Get download stats for feature verson", description = "stats", hidden = true)
    @Schema(hidden = true)
    fun getTotalDownloadStatsForTag(
            @Parameter(name = "feature_version", description = "Feature version (i.e 8, 9, 10...)", required = true)
            @PathParam("feature_version")
            featureVersion: Int,
            @Parameter(name = "release_name", description = "Release Name i.e jdk-11.0.4+11", required = true)
            @PathParam("release_name")
            releaseName: String
    ): Map<String, Long> {
        val release = APIDataStore.getAdoptRepos().getFeatureRelease(featureVersion)
                ?: throw BadRequestException("Unable to find version $featureVersion")

        return getAdoptReleases(release)
                .filter { it.release_name == releaseName }
                .flatMap { it.binaries.asSequence() }
                .map { Pair(it.`package`.name, it.download_count) }
                .toMap()
    }

    private fun getAdoptReleases(release: FeatureRelease): Sequence<Release> {
        return release
                .releases
                .getReleases()
                .filter { it.vendor == Vendor.adoptopenjdk }
    }

    @GET
    @Path("/tracking")
    @Operation(summary = "Get download stats for feature verson", description = "stats", hidden = true)
    @Schema(hidden = true)
    fun tracking(
            @Parameter(name = "feature_version", description = "Feature version (i.e 8, 9, 10...)", schema = Schema(defaultValue = "30"), required = false)
            @QueryParam("feature_version")
            days: Int?
    ): CompletionStage<List<DownloadDiff>> {
        return runAsync {
            val daysSince = days ?: 30
            val since = LocalDateTime.now().minusDays(min(180, daysSince).toLong())
            val githubGrouped = getGithubDownloadStatsByDate(since)
            val dockerGrouped = getDockerDownloadStatsByDate(since)
            return@runAsync calculateDailyDiff(githubGrouped, dockerGrouped)
        }
    }

    private inline fun <reified T> runAsync(crossinline doIt: suspend () -> T): CompletionStage<T> {
        val future = CompletableFuture<T>()
        GlobalScope.launch {
            future.complete(doIt())
        }
        return future
    }

    private fun calculateDailyDiff(githubGrouped: List<Pair<LocalDate, Long>>, dockerGrouped: List<Pair<LocalDate, Long>>): List<DownloadDiff> {
        return githubGrouped
                .union(dockerGrouped)
                .groupBy { it.first }
                .map { grouped -> Pair(grouped.key, grouped.value.map { it.second }.sum()) }
                .sortedBy { it.first }
                .windowed(2, 1, false) {
                    val dayDiff = max(1, ChronoUnit.DAYS.between(it[0].first, it[1].first))
                    val downloadDiff = (it[1].second - it[0].second) / dayDiff
                    DownloadDiff(it[1].first, it[1].second, downloadDiff)
                }
    }

    private suspend fun getGithubDownloadStatsByDate(since: LocalDateTime): List<Pair<LocalDate, Long>> {
        val githubStats = dataStore.getGithubStatsSince(since)

        val githubGrouped = githubStats
                .groupBy { it.date.toLocalDate() }
                .map { grouped -> Pair(grouped.key, grouped.value.maxBy { it.date }!!.downloads) }
        return githubGrouped
    }

    private suspend fun getDockerDownloadStatsByDate(since: LocalDateTime): List<Pair<LocalDate, Long>> {
        val dockerStats = dataStore.getDockerStatsSince(since)

        val dockerGrouped = dockerStats
                .groupBy { it.date.toLocalDate() }
                .map { grouped -> Pair(grouped.key, grouped.value.maxBy { it.date }!!.pulls) }
        return dockerGrouped
    }
}
