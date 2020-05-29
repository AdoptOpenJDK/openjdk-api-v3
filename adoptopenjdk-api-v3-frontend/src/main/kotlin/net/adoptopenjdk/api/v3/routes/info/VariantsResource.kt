package net.adoptopenjdk.api.v3.routes.info

import net.adoptopenjdk.api.v3.models.Variants
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("/v3/info")
@Produces(MediaType.APPLICATION_JSON)
class VariantsResource @Inject constructor(
    @Context
    private val variants: Variants
) {

    @GET
    @Path("/variants")
    // Hide this path as it is only used internally by the website
    @Operation(summary = "Returns information about available variants", hidden = true)
    fun get(): Variants {
        return variants
    }
}
