package net.adoptopenjdk.api.v3.info

import net.adoptopenjdk.api.v3.models.ReleaseInfo
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.json.JSONObject
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
    @APIResponses(value = [
        APIResponse(responseCode = "200", description = "Returns information about available Java variants",
                content = [Content(schema = Schema(example = exampleResult))]
        )
    ])

    fun get(): String {
        return constructedVariant.toString()
    }

    companion object {
      val latestVersion: Int
      val constructedVariant: List <JSONObject>
      val LTSVersions: List <Int>
      val versions: List <Int>
      val latestLTSVersion: Int
      init {
        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        val variants = JSONObject(variantData).getJSONArray("variants")
        versions = variants.map {(it as JSONObject).getInt("version")}.sorted().distinct()
        latestVersion = versions.last()
        LTSVersions = variants.filter{(it as JSONObject).has("lts")}.map {(it as JSONObject).getInt("version")}.sorted().distinct()
        latestLTSVersion = LTSVersions.last()

        constructedVariant = variants.map { entry ->
          val jsonEntry = (entry as JSONObject)
          val vendor = jsonEntry.get("vendor")
          val jvm = jsonEntry.get("jvm")
          val version = jsonEntry.getInt("version")
          val latest = version == latestVersion
          if (latest) {
            jsonEntry.put("latest", true)
          }
          jsonEntry.put("label", "${vendor} ${version}")
          jsonEntry.put("officialName", "${vendor} ${version} with ${jvm}")
        }
      }
      const val exampleResult = """[
  {
    "jvm": "hotspot",
    "vendor": "adoptopenjdk",
    "lts": true,
    "websiteDefault": true,
    "label": "adoptopenjdk 8",
    "officialName": "adoptopenjdk 8 with hotspot",
    "version": 8,
    "searchableName": "openjdk8-hotspot"
  },
  {
    "jvm": "openj9",
    "vendor": "adoptopenjdk",
    "websiteDescriptionLink": "https://www.eclipse.org/openj9",
    "websiteDescription": "Eclipse OpenJ9",
    "lts": true,
    "label": "adoptopenjdk 8",
    "officialName": "adoptopenjdk 8 with openj9",
    "version": 8,
    "searchableName": "openjdk8-openj9"
  },
]"""
    }

}
