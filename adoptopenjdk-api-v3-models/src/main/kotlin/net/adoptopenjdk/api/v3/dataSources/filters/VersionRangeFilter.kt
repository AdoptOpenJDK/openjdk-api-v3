package net.adoptopenjdk.api.v3.dataSources.filters

import net.adoptopenjdk.api.v3.models.VersionData
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange
import java.util.function.Predicate

class VersionRangeFilter(range: String?) : Predicate<VersionData> {

    val rangeMatcher: VersionRange?
    val exactMatcher: DefaultArtifactVersion?


    init {
        // default range behaviour of a solid version is stupid:
        // https://cwiki.apache.org/confluence/display/MAVENOLD/Dependency+Mediation+and+Conflict+Resolution#DependencyMediationandConflictResolution-DependencyVersionRanges
        // so if it is not a range treat ias an exact match
        if (range == null) {
            rangeMatcher = null
            exactMatcher = null
        } else if (!range.startsWith("(") && !range.startsWith("[")) {
            rangeMatcher = null
            exactMatcher = DefaultArtifactVersion(range)
        } else {
            rangeMatcher = VersionRange.createFromVersionSpec(range)
            exactMatcher = null
        }
    }

    override fun test(version: VersionData): Boolean {
        if (exactMatcher != null) {
            return exactMatcher.equals(DefaultArtifactVersion(version.semver))
        } else if (rangeMatcher != null) {
            return rangeMatcher.containsVersion(DefaultArtifactVersion(version.semver))
        } else {
            return true
        }
    }
}