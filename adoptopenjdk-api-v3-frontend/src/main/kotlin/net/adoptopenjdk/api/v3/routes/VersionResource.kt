package net.adoptopenjdk.api.v3.routes

import javax.ws.rs.BadRequestException
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import net.adoptopenjdk.api.v3.models.VersionData
import net.adoptopenjdk.api.v3.parser.FailedToParse
import net.adoptopenjdk.api.v3.parser.VersionParser
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.annotations.jaxrs.PathParam

@Tag(name = "Version")
@Path("/v3/version/")
@Produces(MediaType.APPLICATION_JSON)
class VersionResource {

    @GET
    @Path("/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Parses a java version string", description = "Parses a java version string and returns that data in a structured format")
    @APIResponses(value = [
        APIResponse(responseCode = "400", description = "bad input parameter")
    ])
    fun parseVersion(
        @Parameter(name = "version", description = "Version", required = true)
        @PathParam("version")
        version: String?
    ): VersionData {
        if (version == null) {
            throw BadRequestException("Must provide a version parameter")
        }
        try {
            return VersionParser.parse(version)
        } catch (e: FailedToParse) {
            throw BadRequestException("Unable to parse version")
        }
    }
}
