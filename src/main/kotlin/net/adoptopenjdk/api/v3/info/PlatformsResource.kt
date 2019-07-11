package net.adoptopenjdk.api.v3.info

import net.adoptopenjdk.api.v3.models.ReleaseInfo
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.json.JSONObject
import org.json.JSONArray
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("/v3/info/platforms/")
@Produces(MediaType.APPLICATION_JSON)
class PlatformsResource {

    companion object {
      val platforms: JSONArray
      init {
        val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
        platforms = JSONObject(platformData).getJSONArray("platforms")
      }

        const val exampleResult = """[
    {
      "officialName": "Linux x64",
      "searchableName": "X64_LINUX",
      "logo": "linux.png",
      "attributes": {
        "heap_size": "normal",
        "os": "linux",
        "architecture": "x64"
      },
      "binaryExtension": ".tar.gz",
      "installerExtension": ".run",
      "installCommand": "tar -xf FILENAME",
      "pathCommand": "export PATH=PWD/DIRNAME/bin:PATH",
      "checksumCommand": "sha256sum FILENAME",
      "checksumAutoCommandHint": " (the command must be run on a terminal in the same directory you download the binary file)",
      "checksumAutoCommand": "wget -O- -q -T 1 -t 1 FILEHASHURL | sha256sum -c",
      "osDetectionString": "Linux Mint Debian Fedora FreeBSD Gentoo Haiku Kubuntu OpenBSD Red Hat RHEL SuSE Ubuntu Xubuntu hpwOS webOS Tizen"
    },
    {
      "officialName": "Linux x64 Large Heap",
      "searchableName": "LINUXXL",
      "attributes": {
        "heap_size": "large",
        "os": "linux",
        "architecture": "x64"
      },
      "logo": "linux.png",
      "binaryExtension": ".tar.gz",
      "installerExtension": ".run",
      "installCommand": "tar -xf FILENAME",
      "pathCommand": "export PATH=PWD/DIRNAME/bin:PATH",
      "checksumCommand": "sha256sum FILENAME",
      "checksumAutoCommandHint": " (the command must be run on a terminal in the same directory you download the binary file)",
      "checksumAutoCommand": "wget -O- -q -T 1 -t 1 FILEHASHURL | sha256sum -c",
      "osDetectionString": "not-to-be-detected"
    }
]"""
    }

    @GET
    @Operation(summary = "Returns information about available platforms")
    @APIResponses(value = [
        APIResponse(responseCode = "200", description = "Returns information about available platforms",
                content = [Content(schema = Schema(example = exampleResult))]
        )
    ])
    fun get(): String {
        return platforms.toString()
    }
}
