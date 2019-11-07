package net.adoptopenjdk.api.v3.routes

import net.adoptopenjdk.api.v3.OpenApiDocs
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.dataSources.filters.BinaryFilter
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.dataSources.filters.VersionRangeFilter
import net.adoptopenjdk.api.v3.models.*
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.annotations.jaxrs.PathParam
import org.jboss.resteasy.annotations.jaxrs.QueryParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import kotlin.math.min


@Tag(name = "Assets")
@Path("/assets/")
@Produces(MediaType.APPLICATION_JSON)
class AssetsResource {

    @GET
    @Path("/feature_releases/{feature_version}/{release_type}")
    @Operation(summary = "Returns release information", description = "List of information about builds that match the current query")
    @APIResponses(value = [
        APIResponse(responseCode = "200", description = "search results matching criteria",
                content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = Release::class))]
        ),
        APIResponse(responseCode = "400", description = "bad input parameter")
    ])
    fun get(
            @Parameter(name = "release_type", description = OpenApiDocs.RELEASE_TYPE, required = true)
            @PathParam("release_type")
            release_type: ReleaseType?,

            @Parameter(name = "feature_version", description = OpenApiDocs.FEATURE_RELEASE, required = true,
                    schema = Schema(defaultValue = "8"))
            @PathParam("feature_version")
            version: Int?,

            @Parameter(name = "os", description = "Operating System", required = false)
            @QueryParam("os")
            os: OperatingSystem?,

            @Parameter(name = "architecture", description = "Architecture", required = false)
            @QueryParam("architecture")
            arch: Architecture?,

            @Parameter(name = "image_type", description = "Image Type", required = false)
            @QueryParam("image_type")
            image_type: ImageType?,

            @Parameter(name = "jvm_impl", description = "JVM Implementation", required = false)
            @QueryParam("jvm_impl")
            jvm_impl: JvmImpl?,

            @Parameter(name = "heap_size", description = "Heap Size", required = false)
            @QueryParam("heap_size")
            heap_size: HeapSize?,

            @Parameter(name = "vendor", description = OpenApiDocs.VENDOR, required = false)
            @QueryParam("vendor")
            vendor: Vendor?,

            @Parameter(name = "project", description = "Project", required = false)
            @QueryParam("project")
            project: Project?,

            @Parameter(name = "page_size", description = "Pagination page size", schema = Schema(defaultValue = "10"), required = false)
            @QueryParam("page_size")
            pageSize: Int?,

            @Parameter(name = "page", description = "Pagination page number", schema = Schema(defaultValue = "0"), required = false)
            @QueryParam("page")
            page: Int?,

            @Parameter(name = "sort_order", description = "Result sort order", schema = Schema(defaultValue = "DES"), required = false)
            @QueryParam("sort_order")
            sortOrder: SortOrder?

    ): List<Release> {
        if (release_type == null || version == null) {
            throw BadRequestException("Unrecognised type")
        }
        val order = sortOrder ?: SortOrder.DES

        val releaseFilter = ReleaseFilter(release_type, version, null, vendor, null)
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size)
        val repos = APIDataStore.getAdoptRepos().getFeatureRelease(version)

        if (repos == null) {
            throw NotFoundException()
        }

        val releases = APIDataStore.getAdoptRepos().getFilteredReleases(version, releaseFilter, binaryFilter, order)
        return getPage(pageSize, page, releases)
    }

    @GET
    @Path("/version/{version}")
    @Operation(summary = "Returns release information about the specified version.", description = "List of information about builds that match the current query ")
    @APIResponses(value = [
        APIResponse(responseCode = "200", description = "search results matching criteria",
                content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = Release::class))]
        ),
        APIResponse(responseCode = "400", description = "bad input parameter")
    ])
    fun getReleaseVersion(
            @Parameter(name = "version", description = OpenApiDocs.VERSION_RANGE, required = true)
            @PathParam("version")
            version: String,

            @Parameter(name = "os", description = "Operating System", required = false)
            @QueryParam("os")
            os: OperatingSystem?,

            @Parameter(name = "architecture", description = "Architecture", required = false)
            @QueryParam("architecture")
            arch: Architecture?,

            @Parameter(name = "image_type", description = "Image Type", required = false)
            @QueryParam("image_type")
            image_type: ImageType?,

            @Parameter(name = "jvm_impl", description = "JVM Implementation", required = false)
            @QueryParam("jvm_impl")
            jvm_impl: JvmImpl?,

            @Parameter(name = "heap_size", description = "Heap Size", required = false)
            @QueryParam("heap_size")
            heap_size: HeapSize?,

            @Parameter(name = "vendor", description = OpenApiDocs.VENDOR, required = false)
            @QueryParam("vendor")
            vendor: Vendor?,

            @Parameter(name = "project", description = "Project", required = false)
            @QueryParam("project")
            project: Project?,

            @Parameter(name = "lts", description = "Include only LTS releases", required = false)
            @QueryParam("lts")
            lts: Boolean?,

            @Parameter(name = "release_type", description = OpenApiDocs.RELEASE_TYPE, required = false)
            @QueryParam("release_type")
            release_type: ReleaseType?,

            @Parameter(name = "page_size", description = "Pagination page size", schema = Schema(defaultValue = "20"), required = false)
            @QueryParam("page_size")
            pageSize: Int?,

            @Parameter(name = "page", description = "Pagination page number", schema = Schema(defaultValue = "0"), required = false)
            @QueryParam("page")
            page: Int?,

            @Parameter(name = "sort_order", description = "Result sort order", schema = Schema(defaultValue = "DES"), required = false)
            @QueryParam("sort_order")
            sortOrder: SortOrder?

    ): List<Release> {
        val order = sortOrder ?: SortOrder.DES

        // Require GA due to version range having no meaning for nightlies

        val range = VersionRangeFilter(version)

        val releaseFilter = ReleaseFilter(release_type, null, null, vendor, range)
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size)

        val releases = APIDataStore.getAdoptRepos().getFilteredReleases(releaseFilter, binaryFilter, order)
        return getPage(pageSize, page, releases)
    }


    private fun getPage(pageSize: Int?, page: Int?, releases: Sequence<Release>): List<Release> {
        val pageSizeNum = min(20, (pageSize ?: 10))
        val pageNum = page ?: 0

        val chunked = releases.chunked(pageSizeNum)

        try {
            return chunked.elementAt(pageNum)
        } catch (e: IndexOutOfBoundsException) {
            throw NotFoundException("Page not available")
        }
    }

}
