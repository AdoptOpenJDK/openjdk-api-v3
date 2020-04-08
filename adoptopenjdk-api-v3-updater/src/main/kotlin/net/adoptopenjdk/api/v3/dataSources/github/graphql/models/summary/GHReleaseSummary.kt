package net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper

data class GHReleasesSummary @JsonCreator constructor(
    @JsonProperty("nodes") val releases: List<GHReleaseSummary>,
    @JsonProperty("pageInfo") val pageInfo: PageInfo
) {
    fun getIds(): List<String> {
        return releases.map { it.id }
    }
}

data class GHReleaseSummary @JsonCreator constructor(
    @JsonProperty("id") val id: String,
    @JsonProperty("publishedAt") val publishedAt: String,
    @JsonProperty("updatedAt") val updatedAt: String
) {

    fun getUpdatedTime(): ZonedDateTime {
        return ReleaseMapper.parseDate(updatedAt)
    }
}
