package net.adoptopenjdk.api.v3.info

import net.adoptopenjdk.api.v3.dataSources.AvailablePlatforms
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("/v3/info/available_releases/")
@Produces(MediaType.APPLICATION_JSON)
class AvailableReleasesResource {

    @GET
    @Operation(summary = "Returns information about available releases")
    fun get(): ReleaseInfo {
        return ReleaseInfo(AvailablePlatforms.variants.versions, AvailablePlatforms.variants.ltsVersions, AvailablePlatforms.variants.latestLtsVersion, AvailablePlatforms.variants.latestVersion)
    }
}
