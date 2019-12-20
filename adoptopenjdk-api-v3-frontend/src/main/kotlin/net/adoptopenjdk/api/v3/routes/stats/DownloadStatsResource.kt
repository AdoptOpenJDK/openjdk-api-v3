package net.adoptopenjdk.api.v3.routes.stats

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.models.DownloadStats
import net.adoptopenjdk.api.v3.models.TotalStats
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.LocalDateTime
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


@Path("/stats/downloads")
@Produces(MediaType.APPLICATION_JSON)
@Schema(hidden = true)
class DownloadStatsResource {
    /*


            /v3/stats/downloads/total
            /v3/stats/downloads/total/{version} // where version is a major version (e.g 8)
            /v3/stats/downloads/total/{version}/{tag} // where tag is a release tag
            /v3/stats/downloads/tracking?days=365 // default to 30 days
            /v3/stats/downloads/tracking/{source}?days=365
    */
    private val dataStore = ApiPersistenceFactory.get()


    @GET
    @Path("/total")
    @Operation(summary = "Get download stats", description = "stats", hidden = true)
    @Schema(hidden = true)
    fun getTotalDownloadStats(): DownloadStats {
        return runBlocking {
            val dockerStats = dataStore.getLatestAllDockerStats();

            val githubStats = APIDataStore.variants.versions
                    .map { featureVersion ->
                        dataStore.getLatestGithubStatsForFeatureVersion(featureVersion)
                    }
                    .filterNotNull()

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
            return@runBlocking DownloadStats(LocalDateTime.now(), totalStats, githubBreakdown, dockerBreakdown)
        }
    }
}
