package net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


data class GHReleasesSummary @JsonCreator constructor(@JsonProperty("nodes") val releases: List<GHReleaseSummary>,
                                                      @JsonProperty("pageInfo") val pageInfo: PageInfo) {
    fun getIds(): List<String> {
        return releases.map { it.id }
    }
}

data class GHReleaseSummary @JsonCreator constructor(
        @JsonProperty("id") val id: String,
        @JsonProperty("publishedAt") val publishedAt: String,
        @JsonProperty("updatedAt") val updatedAt: String) {

    fun getUpdatedTime(): LocalDateTime {
        return LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(updatedAt))
    }


}