package net.adoptopenjdk.api.v3.filters

import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import java.util.function.Predicate

class ReleaseFilter(
    private val releaseType: ReleaseType? = null,
    private val featureVersion: Int? = null,
    private val releaseName: String? = null,
    private val vendor: Vendor? = null,
    private val versionRange: VersionRangeFilter? = null,
    private val lts: Boolean? = null
) : Predicate<Release> {
    override fun test(release: Release): Boolean {

        val ltsFilter = if (lts != null) {
            val isLts = APIDataStore.variants.ltsVersions.contains(release.version_data.major)
            lts == isLts
        } else {
            true
        }

        return (releaseType == null || release.release_type == releaseType) &&
            (featureVersion == null || release.version_data.major == featureVersion) &&
            (releaseName == null || release.release_name == releaseName) &&
            (vendor == null || release.vendor == vendor) &&
            (versionRange == null || versionRange.test(release.version_data)) &&
            ltsFilter
    }
}