package net.adoptopenjdk.api.v3.info

import net.adoptopenjdk.api.v3.dataSources.AvailablePlatforms
import net.adoptopenjdk.api.v3.models.Variants
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("/v3/info/variants/")
@Produces(MediaType.APPLICATION_JSON)
class VariantsResource {

    @GET
    @Operation(summary = "Returns information about available Java variants")
    fun get(): Variants {
        return AvailablePlatforms.variants
    }

}
