package net.adoptopenjdk.api.v3.routes.info

import net.adoptopenjdk.api.v3.models.Platforms
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("/v3/info/")
@Produces(MediaType.APPLICATION_JSON)
class PlatformsResource @Inject constructor(
    @Context
    private val platforms: Platforms
) {

    @GET
    @Path("/platforms/")
    // Hide this path as it is only used internally by the website
    @Operation(summary = "Returns information about available platforms", hidden = true)
    fun get(): Platforms {
        return platforms
    }
}
