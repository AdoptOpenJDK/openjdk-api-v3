package net.adoptopenjdk.api.v3.routes

import net.adoptopenjdk.api.v3.OpenApiDocs
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.filters.BinaryFilter
import net.adoptopenjdk.api.v3.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.filters.VersionRangeFilter
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.BinaryAssetView
import net.adoptopenjdk.api.v3.models.HeapSize
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.Project
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
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
import javax.ws.rs.BadRequestException
import javax.ws.rs.GET
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import kotlin.math.min

@Tag(name = "Assets")
@Path("/v3/assets/")
@Produces(MediaType.APPLICATION_JSON)
class AssetsResource {

    @GET
    @Path("/feature_releases/{feature_version}/{release_type}")
    @Operation(summary = "Returns release information",
               description = "List of information about builds that match the current query"
    )
    @APIResponses(value = [
        APIResponse(responseCode = "200", description = "search results matching criteria",
                    content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = Release::class))]
        ),
        APIResponse(responseCode = "400", description = "bad input parameter")
    ]
    )
    fun get(
        @Parameter(name = "release_type", description = OpenApiDocs.RELEASE_TYPE, required = true)
        @PathParam("release_type")
        release_type: ReleaseType?,

        @Parameter(name = "feature_version", description = OpenApiDocs.FEATURE_RELEASE, required = true,
                   schema = Schema(defaultValue = "8", type = SchemaType.INTEGER)
        )
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

        @Parameter(name = "project", description = "Project", schema = Schema(defaultValue = "jdk",
                                                                              enumeration = ["jdk", "valhalla", "metropolis", "jfr"], required = false
        ), required = false
        )
        @QueryParam("project")
        project: Project?,

        @Parameter(name = "page_size", description = "Pagination page size",
                   schema = Schema(defaultValue = "10", type = SchemaType.INTEGER), required = false
        )
        @QueryParam("page_size")
        pageSize: Int?,

        @Parameter(name = "page", description = "Pagination page number",
                   schema = Schema(defaultValue = "0", type = SchemaType.INTEGER), required = false
        )
        @QueryParam("page")
        page: Int?,

        @Parameter(name = "sort_order", description = "Result sort order", required = false)
        @QueryParam("sort_order")
        sortOrder: SortOrder?

    ): List<Release> {
        if (release_type == null || version == null) {
            throw BadRequestException("Unrecognised type")
        }
        val order = sortOrder ?: SortOrder.DESC

        val releaseFilter = ReleaseFilter(releaseType = release_type, featureVersion = version, vendor = vendor)
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size, project)
        val repos = APIDataStore.getAdoptRepos().getFeatureRelease(version)

        if (repos == null) {
            throw NotFoundException()
        }

        val releases = APIDataStore
            .getAdoptRepos()
            .getFilteredReleases(version, releaseFilter, binaryFilter, order)

        return getPage(pageSize, page, releases)
    }

    @GET
    @Path("/version/{version}")
    @Operation(summary = "Returns release information about the specified version.",
               description = "List of information about builds that match the current query "
    )
    @APIResponses(value = [
        APIResponse(responseCode = "200", description = "search results matching criteria",
                    content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = Release::class))]
        ),
        APIResponse(responseCode = "400", description = "bad input parameter")
    ]
    )
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

        @Parameter(name = "project", description = "Project", schema = Schema(defaultValue = "jdk",
                                                                              enumeration = ["jdk", "valhalla", "metropolis", "jfr"], required = false
        ), required = false
        )
        @QueryParam("project")
        project: Project?,

        @Parameter(name = "lts", description = "Include only LTS releases", required = false)
        @QueryParam("lts")
        lts: Boolean?,

        @Parameter(name = "release_type", description = OpenApiDocs.RELEASE_TYPE, required = false)
        @QueryParam("release_type")
        release_type: ReleaseType?,

        @Parameter(name = "page_size", description = "Pagination page size",
                   schema = Schema(defaultValue = "20", type = SchemaType.INTEGER), required = false
        )
        @QueryParam("page_size")
        pageSize: Int?,

        @Parameter(name = "page", description = "Pagination page number",
                   schema = Schema(defaultValue = "0", type = SchemaType.INTEGER), required = false
        )
        @QueryParam("page")
        page: Int?,

        @Parameter(name = "sort_order", description = "Result sort order", required = false)
        @QueryParam("sort_order")
        sortOrder: SortOrder?

    ): List<Release> {
        val order = sortOrder ?: SortOrder.DESC

        // Require GA due to version range having no meaning for nightlies

        val range = VersionRangeFilter(version)

        val releaseFilter = ReleaseFilter(releaseType = release_type, vendor = vendor, versionRange = range, lts = lts)
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size, project)

        val releases = APIDataStore
            .getAdoptRepos()
            .getFilteredReleases(releaseFilter, binaryFilter, order)

        return getPage(pageSize, page, releases)
    }

    private fun getPage(pageSize: Int?, page: Int?, releases: Sequence<Release>): List<Release> {
        val pageSizeNum = min(20, (pageSize ?: 10))
        val pageNum = page ?: 0

        val chunked = releases.chunked(pageSizeNum)

        try {
            val res = chunked.elementAt(pageNum)
            return res
        } catch (e: IndexOutOfBoundsException) {
            throw NotFoundException("Page not available")
        }
    }

    data class binaryPermutation(
        val arch: Architecture,
        val heapSize: HeapSize,
        val imageType: ImageType,
        val os: OperatingSystem
    )

    @GET
    @Path("/latest/{feature_version}/{jvm_impl}")
    @Operation(summary = "Returns list of latest assets for the given feature version and jvm impl")
    fun getLatestAssets(

        @Parameter(name = "feature_version", description = OpenApiDocs.FEATURE_RELEASE, required = true,
                   schema = Schema(defaultValue = "8", type = SchemaType.INTEGER)
        )
        @PathParam("feature_version")
        version: Int,

        @Parameter(name = "jvm_impl", description = "JVM Implementation", required = true)
        @PathParam("jvm_impl")
        jvm_impl: JvmImpl

    ): List<BinaryAssetView> {
        val releaseFilter = ReleaseFilter(ReleaseType.ga, featureVersion = version, vendor = Vendor.adoptopenjdk)
        val binaryFilter = BinaryFilter(null, null, null, jvm_impl, null, null)
        val releases = APIDataStore
            .getAdoptRepos()
            .getFilteredReleases(version, releaseFilter, binaryFilter, SortOrder.ASC)

        return releases
            .flatMap { release ->
                release.binaries
                    .asSequence()
                    .map { Pair(release, it) }
            }
            .associateBy {
                binaryPermutation(it.second.architecture, it.second.heap_size, it.second.image_type, it.second.os)
            }
            .values
            .map { BinaryAssetView(it.first.release_name, it.second, it.first.version_data) }
            .toList()
    }
}
