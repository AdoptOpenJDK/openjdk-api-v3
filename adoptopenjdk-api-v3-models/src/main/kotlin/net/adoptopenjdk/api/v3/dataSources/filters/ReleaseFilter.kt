package net.adoptopenjdk.api.v3.dataSources.filters

import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import java.util.function.Predicate

class ReleaseFilter(private val releaseType: ReleaseType?,
                    private val featureVersion: Int?,
                    private val releaseName: String?,
                    private val vendor: Vendor?,
                    private val versionRange: VersionRangeFilter?) : Predicate<Release> {

    override fun test(release: Release): Boolean {
        return (releaseType == null || release.release_type == releaseType) &&
                (featureVersion == null || release.version_data.major == featureVersion) &&
                (releaseName == null || release.release_name == releaseName) &&
                (vendor == null || release.vendor == vendor) &&
                (versionRange == null || versionRange.test(release.version_data))
    }

}
