package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

class VersionData {

    val major: Int
    val minor: Int
    val security: Int
    val pre: String?
    val adopt_build_number: Int

    @Schema(example = "11.0.0+28")
    val semver: String

    @Schema(example = "11.0.4+10-201907081820")
    val openjdk_version: String
    val build: Int
    val optional: String?

    constructor(major: Int, minor: Int, security: Int, pre: String?, adopt_build_number: Int, semver: String, build: Int, optional: String?, openjdk_version: String) {
        this.minor = minor
        this.security = security
        this.pre = pre
        this.adopt_build_number = adopt_build_number
        this.major = major
        this.semver = semver
        this.build = build
        this.optional = optional
        this.openjdk_version = openjdk_version
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VersionData

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (security != other.security) return false
        if (pre != other.pre) return false
        if (adopt_build_number != other.adopt_build_number) return false
        if (semver != other.semver) return false
        if (openjdk_version != other.openjdk_version) return false
        if (build != other.build) return false
        if (optional != other.optional) return false

        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + security
        result = 31 * result + (pre?.hashCode() ?: 0)
        result = 31 * result + adopt_build_number
        result = 31 * result + semver.hashCode()
        result = 31 * result + openjdk_version.hashCode()
        result = 31 * result + build
        result = 31 * result + (optional?.hashCode() ?: 0)
        return result
    }

}
