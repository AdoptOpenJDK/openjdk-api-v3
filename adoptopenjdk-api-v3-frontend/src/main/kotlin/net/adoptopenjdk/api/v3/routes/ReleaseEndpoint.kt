package net.adoptopenjdk.api.v3.routes

import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.SortMethod
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.filters.BinaryFilter
import net.adoptopenjdk.api.v3.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.filters.VersionRangeFilter
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.HeapSize
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.Project
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class ReleaseEndpoint
@Inject
constructor(private val apiDataStore: APIDataStore) {
    fun getReleases(
        sortOrder: SortOrder?,
        sortMethod: SortMethod?,
        version: String?,
        release_type: ReleaseType?,
        vendor: Vendor?,
        lts: Boolean?,
        os: OperatingSystem?,
        arch: Architecture?,
        image_type: ImageType?,
        jvm_impl: JvmImpl?,
        heap_size: HeapSize?,
        project: Project?
    ): Sequence<Release> {
        val order = sortOrder ?: SortOrder.DESC
        val vendorNonNull = vendor ?: Vendor.adoptopenjdk
        val sortMethod = sortMethod ?: SortMethod.DEFAULT

        val range = VersionRangeFilter(version)

        val releaseFilter = ReleaseFilter(releaseType = release_type, vendor = vendorNonNull, versionRange = range, lts = lts)
        val binaryFilter = BinaryFilter(os = os, arch = arch, imageType = image_type, jvmImpl = jvm_impl, heapSize = heap_size, project = project)

        return apiDataStore
            .getAdoptRepos()
            .getFilteredReleases(releaseFilter, binaryFilter, order, sortMethod)
    }
}
