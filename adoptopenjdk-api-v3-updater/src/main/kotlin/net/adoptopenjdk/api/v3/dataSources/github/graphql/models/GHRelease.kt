package net.adoptopenjdk.api.v3.dataSources.github.graphql.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class GHReleases @JsonCreator constructor(
    @JsonProperty("nodes") val releases: List<GHRelease>,
    @JsonProperty("pageInfo") val pageInfo: PageInfo
)

data class GHReleaseResult @JsonCreator constructor(
    @JsonProperty("node") val release: GHRelease,
    @JsonProperty("rateLimit") override val rateLimit: RateLimit
) : HasRateLimit(rateLimit)

data class GHRelease @JsonCreator constructor(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("isPrerelease") val isPrerelease: Boolean,
    @JsonProperty("prerelease") val prerelease: Boolean?,
    @JsonProperty("publishedAt") val publishedAt: String,
    @JsonProperty("updatedAt") val updatedAt: String,
    @JsonProperty("releaseAssets") val releaseAssets: GHAssets,
    @JsonProperty("resourcePath") val resourcePath: String,
    @JsonProperty("url") val url: String
)
