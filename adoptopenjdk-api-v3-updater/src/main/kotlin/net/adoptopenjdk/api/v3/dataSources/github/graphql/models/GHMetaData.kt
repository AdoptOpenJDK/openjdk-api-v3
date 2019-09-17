package net.adoptopenjdk.api.v3.dataSources.github.graphql.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.models.*

data class GHVersion @JsonCreator constructor(
        @JsonProperty("major") val major: Int,
        @JsonProperty("minor") val minor: Int,
        @JsonProperty("security") val security: Int,
        @JsonProperty("pre") val pre: String?,
        @JsonProperty("adopt_build_number") val adopt_build_number: Int,
        @JsonProperty("version") val version: String,
        @JsonProperty("build") val build: Int,
        @JsonProperty("opt") val opt: String?) {
    fun toApiVersion(): VersionData {
        return VersionData(major, minor, security, pre, adopt_build_number, build, opt, version)
    }
}

data class GHMetaData @JsonCreator constructor(
        @JsonProperty("WARNING") val warning: String,
        @JsonProperty("os") val os: OperatingSystem,
        @JsonProperty("arch") val arch: Architecture,
        @JsonProperty("variant") val variant: JvmImpl,
        @JsonProperty("version") val version: GHVersion,
        @JsonProperty("scmRef") val scmRef: String,
        @JsonProperty("version_data") val version_data: String,
        @JsonProperty("binary_type") val binary_type: ImageType,
        @JsonProperty("sha256") val sha256: String)