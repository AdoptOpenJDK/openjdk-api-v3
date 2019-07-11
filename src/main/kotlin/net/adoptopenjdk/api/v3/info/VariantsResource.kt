package net.adoptopenjdk.api.v3.info

import net.adoptopenjdk.api.v3.models.ReleaseInfo
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
@Path("/info/variants/")
@Produces(MediaType.APPLICATION_JSON)
class VariantsResource {

    companion object {
        const val exampleResult = """[
    {
      "searchableName": "openjdk8-hotspot",
      "officialName": "OpenJDK 8 with HotSpot",
      "jvm": "HotSpot",
      "label": "OpenJDK 8",
      "lts": true,
      "default": true
    },
    {
      "searchableName": "openjdk8-openj9",
      "officialName": "OpenJDK 8 with Eclipse OpenJ9",
      "description": "Eclipse OpenJ9",
      "jvm": "OpenJ9",
      "label": "OpenJDK 8",
      "lts": true,
      "descriptionLink": "https://www.eclipse.org/openj9"
    }
]"""
    }

    @GET
    @Operation(summary = "Returns information about available Java variants")
    @APIResponses(value = [
        APIResponse(responseCode = "200", description = "Returns information about available Java variants",
                content = [Content(schema = Schema(example = exampleResult))]
        )
    ])
    private fun get(): ReleaseInfo {
        return ReleaseInfo(listOf(), 11, 13)
    }


}