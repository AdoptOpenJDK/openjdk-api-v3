package net.adoptopenjdk.api.v3.routes

import net.adoptopenjdk.api.v3.OpenApiDocs
import net.adoptopenjdk.api.v3.Pagination.defaultPageSize
import net.adoptopenjdk.api.v3.Pagination.getPage
import net.adoptopenjdk.api.v3.Pagination.maxPageSize
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.SortMethod
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
import org.eclipse.microprofile.metrics.annotation.Timed
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
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalQuery
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.BadRequestException
import javax.ws.rs.GET
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.ServerErrorException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Tag(name = "Assets")
@Path("/v3/assets/")
@Produces(MediaType.APPLICATION_JSON)
@Timed
@ApplicationScoped
class AssetsResource
@Inject
constructor(
    private val apiDataStore: APIDataStore
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    @GET
    @Path("/feature_releases/{feature_version}/{release_type}")
    @Operation(
        summary = "Returns release information",
        description = "List of information about builds that match the current query"
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200", description = "search results matching criteria",
                content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = Release::class))]
            ),
            APIResponse(responseCode = "400", description = "bad input parameter")
        ]
    )
    fun get(
        @Parameter(name = "release_type", description = OpenApiDocs.RELEASE_TYPE, required = true)
        @PathParam("release_type")
        release_type: ReleaseType?,

        @Parameter(
            name = "feature_version", description = OpenApiDocs.FEATURE_RELEASE, required = true,
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

        @Parameter(name = "project", description = "Project", required = false)
        @QueryParam("project")
        project: Project?,

        @Parameter(
            name = "before",
            description = "Return binaries whose updated_at is before the given date/time. When a date is given the match is inclusive of the given day.",
            schema = Schema(type = SchemaType.STRING),
            example = "2020-01-21, 2020-01-21T10:15:30, 20200121, 2020-12-21T10:15:30Z, 2020-12-21+01:00",
            required = false
        )
        @QueryParam("before")
        before: String?,

        @Parameter(
            name = "page_size", description = "Pagination page size",
            schema = Schema(defaultValue = defaultPageSize, maximum = maxPageSize, type = SchemaType.INTEGER), required = false
        )
        @QueryParam("page_size")
        pageSize: Int?,

        @Parameter(
            name = "page", description = "Pagination page number",
            schema = Schema(defaultValue = "0", type = SchemaType.INTEGER), required = false
        )
        @QueryParam("page")
        page: Int?,

        @Parameter(name = "sort_order", description = "Result sort order", required = false)
        @QueryParam("sort_order")
        sortOrder: SortOrder?,

        @Parameter(name = "sort_method", description = "Result sort method", required = false)
        @QueryParam("sort_method")
        sortMethod: SortMethod?

    ): List<Release> {
        val order = sortOrder ?: SortOrder.DESC
        val sortMethod = sortMethod ?: SortMethod.DEFAULT

        val beforeParsed = parseDate(before)

        val releaseFilter = ReleaseFilter(releaseType = release_type, featureVersion = version, vendor = vendor)
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size, project, beforeParsed)
        val repos = apiDataStore.getAdoptRepos().getFeatureRelease(version!!)

        if (repos == null) {
            throw NotFoundException()
        }

        val releases = apiDataStore
            .getAdoptRepos()
            .getFilteredReleases(version, releaseFilter, binaryFilter, order, sortMethod)

        return getPage(pageSize, page, releases)
    }

    @GET
    @Path("/release_name/{vendor}/{release_name}")
    @Operation(
        summary = "Returns release information",
        description = "List of releases with the given release name"
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200", description = "Release with the given vendor and name"
            ),
            APIResponse(responseCode = "400", description = "bad input parameter"),
            APIResponse(responseCode = "404", description = "no releases match the request"),
            APIResponse(responseCode = "500", description = "multiple releases match the request")
        ]
    )
    fun get(
        @Parameter(name = "vendor", description = OpenApiDocs.VENDOR, required = false)
        @PathParam("vendor")
        vendor: Vendor?,

        @Parameter(name = "release_name", description = "Name of the release i.e ", required = true)
        @PathParam("release_name")
        releaseName: String?,

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

        @Parameter(name = "project", description = "Project", required = false)
        @QueryParam("project")
        project: Project?
    ): Release {
        if (releaseName == null || releaseName.trim().isEmpty()) {
            throw BadRequestException("Must provide a releaseName")
        }

        if (vendor == null) {
            throw BadRequestException("Must provide a vendor")
        }

        val releaseFilter = ReleaseFilter(vendor = vendor, releaseName = releaseName.trim())
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size, project)

        val releases = apiDataStore
            .getAdoptRepos()
            .getFilteredReleases(
                releaseFilter,
                binaryFilter,
                SortOrder.DESC,
                SortMethod.DEFAULT
            )
            .toList()

        return when {
            releases.isEmpty() -> {
                throw NotFoundException("No releases found")
            }
            releases.size > 1 -> {
                throw ServerErrorException("Multiple releases match request", Response.Status.INTERNAL_SERVER_ERROR)
            }
            else -> {
                releases[0]
            }
        }
    }

    private fun parseDate(rawDate: String?): ZonedDateTime? {
        return if (rawDate != null) {
            try {
                val date = LocalDate.parse(rawDate, DateTimeFormatter.ISO_DATE)
                return date.plusDays(1).atStartOfDay(TimeSource.ZONE)
            } catch (e: DateTimeParseException) {
                // NOP
            }

            try {
                val parsedDate = DateTimeFormatter.ISO_DATE_TIME.parseBest(
                    rawDate,
                    TemporalQuery { p0 -> ZonedDateTime.from(p0) },
                    TemporalQuery { p0 -> OffsetDateTime.from(p0) },
                    TemporalQuery { p0 -> LocalDateTime.from(p0) }
                )

                return when (parsedDate) {
                    is LocalDateTime -> parsedDate.atZone(TimeSource.ZONE)
                    is OffsetDateTime -> parsedDate.atZoneSameInstant(TimeSource.ZONE)
                    is ZonedDateTime -> parsedDate
                    else -> null
                }
            } catch (e: DateTimeParseException) {
                // NOP
            }

            try {
                val date = LocalDate.parse(rawDate, DateTimeFormatter.BASIC_ISO_DATE)
                return date.plusDays(1).atStartOfDay(TimeSource.ZONE)
            } catch (e: DateTimeParseException) {
                LOGGER.info("Failed to parse date: $rawDate", e)
                throw BadRequestException("Cannot parse date")
            }
        } else {
            null
        }
    }

    @GET
    @Path("/version/{version}")
    @Operation(
        summary = "Returns release information about the specified version.",
        description = "List of information about builds that match the current query "
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200", description = "search results matching criteria",
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

        @Parameter(name = "project", description = "Project", required = false)
        @QueryParam("project")
        project: Project?,

        @Parameter(name = "lts", description = "Include only LTS releases", required = false)
        @QueryParam("lts")
        lts: Boolean?,

        @Parameter(name = "release_type", description = OpenApiDocs.RELEASE_TYPE, required = false)
        @QueryParam("release_type")
        release_type: ReleaseType?,

        @Parameter(
            name = "page_size", description = "Pagination page size",
            schema = Schema(defaultValue = defaultPageSize, maximum = maxPageSize, type = SchemaType.INTEGER), required = false
        )
        @QueryParam("page_size")
        pageSize: Int?,

        @Parameter(
            name = "page", description = "Pagination page number",
            schema = Schema(defaultValue = "0", type = SchemaType.INTEGER), required = false
        )
        @QueryParam("page")
        page: Int?,

        @Parameter(name = "sort_order", description = "Result sort order", required = false)
        @QueryParam("sort_order")
        sortOrder: SortOrder?,

        @Parameter(name = "sort_method", description = "Result sort method", required = false)
        @QueryParam("sort_method")
        sortMethod: SortMethod?
    ): List<Release> {
        val order = sortOrder ?: SortOrder.DESC
        val sortMethod = sortMethod ?: SortMethod.DEFAULT

        val range = VersionRangeFilter(version)

        val releaseFilter = ReleaseFilter(releaseType = release_type, vendor = vendor, versionRange = range, lts = lts)
        val binaryFilter = BinaryFilter(os = os, arch = arch, imageType = image_type, jvmImpl = jvm_impl, heapSize = heap_size, project = project)

        val releases = apiDataStore
            .getAdoptRepos()
            .getFilteredReleases(releaseFilter, binaryFilter, order, sortMethod)

        return getPage(pageSize, page, releases)
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

        @Parameter(
            name = "feature_version", description = OpenApiDocs.FEATURE_RELEASE, required = true,
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
        val releases = apiDataStore
            .getAdoptRepos()
            .getFilteredReleases(version, releaseFilter, binaryFilter, SortOrder.ASC, SortMethod.DEFAULT)

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
