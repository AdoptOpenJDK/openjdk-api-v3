package net.adoptopenjdk.api.v3.routes

import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.OpenApiDocs
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.filters.BinaryFilter
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.models.*
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.annotations.jaxrs.PathParam
import java.net.URI
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


@Tag(name = "Binary")
@Path("binary/")
@Produces(MediaType.APPLICATION_JSON)
class BinaryResource {


    @GET
    @Path("version/{release_name}/{os}/{arch}/{image_type}/{jvm_impl}/{heap_size}/{vendor}")
    @Produces("application/octet-stream")
    @Operation(summary = "Redirects to the binary that matches your current query", description = "Redirects to the binary that matches your current query")
    @APIResponses(value = [
        APIResponse(responseCode = "307", description = "link to binary that matches your current query"),
        APIResponse(responseCode = "400", description = "bad input parameter"),
        APIResponse(responseCode = "404", description = "No matching binary found")
    ])
    fun returnBinaryByVersion(
            @Parameter(name = "os", description = "Operating System", required = true)
            @PathParam("os")
            os: OperatingSystem?,

            @Parameter(name = "arch", description = "Architecture", required = true)
            @PathParam("arch")
            arch: Architecture?,

            @Parameter(name = "release_name", description = OpenApiDocs.RELASE_NAME, required = true,
                    schema = Schema(defaultValue = "jdk-11.0.4+11"))
            @PathParam("release_name")
            release_name: String?,

            @Parameter(name = "image_type", description = "Image Type", required = true)
            @PathParam("image_type")
            image_type: ImageType?,

            @Parameter(name = "jvm_impl", description = "JVM Implementation", required = true)
            @PathParam("jvm_impl")
            jvm_impl: JvmImpl?,

            @Parameter(name = "heap_size", description = "Heap Size", required = true)
            @PathParam("heap_size")
            heap_size: HeapSize?,

            @Parameter(name = "vendor", description = OpenApiDocs.VENDOR, required = true)
            @PathParam("vendor")
            vendor: Vendor?
    ): Response {

        val releaseFilter = ReleaseFilter(null, null, release_name, vendor, null)
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size)
        val releases = APIDataStore.getAdoptRepos().getFilteredReleases(releaseFilter, binaryFilter).toList()

        return formResponse(releases)

    }


    @GET
    @Path("latest/{feature_version}/{release_type}/{os}/{arch}/{image_type}/{jvm_impl}/{heap_size}/{vendor}")
    @Produces("application/octet-stream")
    @Operation(summary = "Redirects to the binary that matches your current query", description = "Redirects to the binary that matches your current query")
    @APIResponses(value = [
        APIResponse(responseCode = "307", description = "link to binary that matches your current query"),
        APIResponse(responseCode = "400", description = "bad input parameter"),
        APIResponse(responseCode = "404", description = "No matching binary found")
    ])
    fun returnBinary(
            @Parameter(name = "feature_version", description = OpenApiDocs.FEATURE_RELEASE, required = true,
                    schema = Schema(defaultValue = "8"))
            @PathParam("feature_version")
            version: Int?,

            @Parameter(name = "release_type", description = OpenApiDocs.RELEASE_TYPE, required = true)
            @PathParam("release_type")
            release_type: ReleaseType?,

            @Parameter(name = "os", description = "Operating System", required = true)
            @PathParam("os")
            os: OperatingSystem?,

            @Parameter(name = "arch", description = "Architecture", required = true)
            @PathParam("arch")
            arch: Architecture?,

            @Parameter(name = "image_type", description = "Image Type", required = true)
            @PathParam("image_type")
            image_type: ImageType?,

            @Parameter(name = "jvm_impl", description = "JVM Implementation", required = true)
            @PathParam("jvm_impl")
            jvm_impl: JvmImpl?,

            @Parameter(name = "heap_size", description = "Heap Size", required = true)
            @PathParam("heap_size")
            heap_size: HeapSize?,

            @Parameter(name = "vendor", description = OpenApiDocs.VENDOR, required = true)
            @PathParam("vendor")
            vendor: Vendor?
    ): Response {
        val releaseFilter = ReleaseFilter(release_type, version, null, vendor, null)
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size)
        val releases = APIDataStore.getAdoptRepos().getFilteredReleases(releaseFilter, binaryFilter).toList()

        val comparator = compareBy<Release> { it.version_data.major }
                .thenBy { it.version_data.minor }
                .thenBy { it.version_data.security }
                .thenBy { it.version_data.pre }
                .thenBy { it.version_data.build }
                .thenBy { it.version_data.adopt_build_number }
                .thenBy { it.version_data.optional }


        val release = releases.sortedWith(comparator).lastOrNull()

        return formResponse(if (release == null) emptyList() else listOf(release))
    }

    private fun formResponse(releases: List<Release>): Response {
        if (releases.size == 0) {
            return formErrorResponse(Response.Status.NOT_FOUND, "No releases match the request")
        } else if (releases.size > 1) {
            val versions = releases
                    .map { it.release_name }
            return formErrorResponse(Response.Status.BAD_REQUEST, "Multiple releases match request: $versions")
        } else {
            val binaries = releases.get(0).binaries
            val packages = binaries
                    .map { it.`package` }
                    .filterNotNull()

            if (packages.size == 0) {
                return formErrorResponse(Response.Status.NOT_FOUND, "No binaries match the request")
            } else if (packages.size > 1) {
                val names = packages.map { it.name }
                return formErrorResponse(Response.Status.BAD_REQUEST, "Multiple binaries match request: $names")
            } else {
                return Response.temporaryRedirect(URI.create(packages.first().link)).build()
            }
        }
    }

    private fun formErrorResponse(status: Response.Status, message: String): Response {
        return Response
                .status(status)
                .entity(JsonMapper.mapper.writeValueAsString(APIError(message)))
                .build()
    }

}
