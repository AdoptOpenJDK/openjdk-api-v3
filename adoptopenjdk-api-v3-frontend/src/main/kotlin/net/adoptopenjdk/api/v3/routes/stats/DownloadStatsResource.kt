package net.adoptopenjdk.api.v3.routes.stats

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.models.DownloadDiff
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.StatsSource
import net.adoptopenjdk.api.v3.models.Vendor
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.jboss.resteasy.annotations.jaxrs.PathParam
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import javax.ws.rs.BadRequestException
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


@Path("/stats/downloads")
@Produces(MediaType.APPLICATION_JSON)
@Schema(hidden = true)
class DownloadStatsResource {
    private val statsInterface = net.adoptopenjdk.api.v3.DownloadStatsInterface()

    @GET
    @Path("/total")
    @Operation(summary = "Get download stats", description = "stats", hidden = true)
    @Schema(hidden = true)
    fun getTotalDownloadStats(): CompletionStage<net.adoptopenjdk.api.v3.models.DownloadStats> {
        return runAsync {
            return@runAsync statsInterface.getTotalDownloadStats()
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
                .map { grouped ->
                    Pair(grouped.release_name, grouped.binaries.map {
                        it.download_count + ((it.installer?.download_count) ?: 0L)
                    }.sum())
                }
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
                .flatMap {
                    val archive = Pair(it.`package`.name, it.download_count)
                    if (it.installer != null) {
                        sequenceOf(archive, Pair(it.installer!!.name, it.installer!!.download_count))
                    } else {
                        sequenceOf(archive)
                    }
                }
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
            @Parameter(name = "days", description = "Number of days to display", schema = Schema(defaultValue = "30"), required = false)
            @QueryParam("days")
            days: Int?,
            @Parameter(name = "source", description = "Stats data source", schema = Schema(defaultValue = "all"), required = false)
            @QueryParam("source")
            source: StatsSource?,
            @Parameter(name = "feature_version", description = "Feature version (i.e 8, 9, 10...), only valid on github source requests", required = false)
            @QueryParam("feature_version")
            featureVersion: Int?,
            @Parameter(name = "docker_repo", description = "Docker repo to filter stats by", required = false)
            @QueryParam("docker_repo")
            dockerRepo: String?
    ): CompletionStage<List<DownloadDiff>> {
        return runAsync {
            if (featureVersion != null && source != StatsSource.github) {
                throw BadRequestException(Response.status(400, "feature_version can only be used with source=github").build())
            }

            if (dockerRepo != null && source != StatsSource.dockerhub) {
                throw BadRequestException(Response.status(400, "docker_repo can only be used with source=dockerhub").build())
            }

            return@runAsync statsInterface.getTrackingStats(days, source, featureVersion, dockerRepo)
        }
    }

    private inline fun <reified T> runAsync(crossinline doIt: suspend () -> T): CompletionStage<T> {
        val future = CompletableFuture<T>()
        GlobalScope.launch {
            try {

                future.complete(doIt())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

}
