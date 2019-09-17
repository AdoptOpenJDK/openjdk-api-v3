package net.adoptopenjdk.api.v3.routes.info

import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.models.Platforms
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("info/platforms/")
@Produces(MediaType.APPLICATION_JSON)
@Schema(hidden = true)
class PlatformsResource {

    @GET
    @Operation(summary = "Returns information about available platforms")
    @Schema(hidden = true)
    fun get(): Platforms {
        return APIDataStore.platforms
    }
}
