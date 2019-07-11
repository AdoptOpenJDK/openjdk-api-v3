package net.adoptopenjdk.api.v3.info

import com.fasterxml.jackson.databind.ObjectMapper
import net.adoptopenjdk.api.v3.dataSources.AvailablePlatforms
import net.adoptopenjdk.api.v3.models.Platforms
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("/v3/info/platforms/")
@Produces(MediaType.APPLICATION_JSON)
class PlatformsResource {

    @GET
    @Operation(summary = "Returns information about available platforms")
    fun get(): Platforms {
        return AvailablePlatforms.platforms
    }
}
