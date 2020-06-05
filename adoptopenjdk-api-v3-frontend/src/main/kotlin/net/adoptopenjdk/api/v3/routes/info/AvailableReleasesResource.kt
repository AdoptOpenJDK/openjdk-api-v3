package net.adoptopenjdk.api.v3.routes.info

import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.ReleaseType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("/v3/info/")
@Produces(MediaType.APPLICATION_JSON)
class AvailableReleasesResource {

    private var releaseInfo: ReleaseInfo = formReleaseInfo()

    @GET
    @Path("/available_releases/")
    @Operation(summary = "Returns information about available releases")
    fun get(): ReleaseInfo {
        return releaseInfo
    }

    final fun formReleaseInfo(): ReleaseInfo {
        val gaReleases = APIDataStore.getAdoptRepos()
            .allReleases
            .getReleases()
            .filter { it.release_type == ReleaseType.ga }
            .toList()

        val availableReleases = gaReleases
            .map { it.version_data.major }
            .distinct()
            .sorted()
            .toList()
            .toTypedArray()
        val mostRecentFeatureRelease: Int = availableReleases.last()

        val availableLtsReleases: Array<Int> = gaReleases
            .filter { APIDataStore.variants.ltsVersions.contains(it.version_data.major) }
            .map { it.version_data.major }
            .distinct()
            .sorted()
            .toList()
            .toTypedArray()
        val mostRecentLts = availableLtsReleases.last()

        val mostRecentFeatureVersion: Int = APIDataStore.getAdoptRepos()
            .allReleases
            .getReleases()
            .map { it.version_data.major }
            .distinct()
            .sorted()
            .last()

        return ReleaseInfo(
            availableReleases,
            availableLtsReleases,
            mostRecentLts,
            mostRecentFeatureRelease,
            mostRecentFeatureVersion
        )
    }
}
