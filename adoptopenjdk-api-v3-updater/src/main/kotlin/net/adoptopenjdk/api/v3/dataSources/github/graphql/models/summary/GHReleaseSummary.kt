package net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GithubIdDeserializer
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.models.GithubId
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import java.time.ZonedDateTime


data class GHReleasesSummary @JsonCreator constructor(@JsonProperty("nodes") val releases: List<GHReleaseSummary>,
                                                      @JsonProperty("pageInfo") val pageInfo: PageInfo) {
    fun getIds(): List<GithubId> {
        return releases.map { it.id }
    }
}

data class GHReleaseSummary @JsonCreator constructor(
        @JsonProperty("id")
        @JsonDeserialize(using = GithubIdDeserializer::class)
        val id: GithubId,
        @JsonProperty("publishedAt") val publishedAt: String,
        @JsonProperty("updatedAt") val updatedAt: String) {

    fun getUpdatedTime(): ZonedDateTime {
        return ReleaseMapper.parseDate(updatedAt)
    }


}