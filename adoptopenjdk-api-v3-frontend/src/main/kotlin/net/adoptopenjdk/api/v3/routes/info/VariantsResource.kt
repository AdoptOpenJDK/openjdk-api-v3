package net.adoptopenjdk.api.v3.routes.info

import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.models.Variants
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("info")
@Produces(MediaType.APPLICATION_JSON)
class VariantsResource {

    @GET
    @Path("variants")
    @Operation(summary = "Returns information about available variants")
    fun get(): Variants {
        return APIDataStore.variants
    }

}
