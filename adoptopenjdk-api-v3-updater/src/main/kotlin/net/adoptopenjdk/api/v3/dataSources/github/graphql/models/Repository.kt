package net.adoptopenjdk.api.v3.dataSources.github.graphql.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.models.Release

data class Repository @JsonCreator constructor(@JsonProperty("releases") val releases: GHReleases) {
    suspend fun getReleases(): List<Release> {
        return releases.releases.map({ it.toAdoptRelease() })
    }
}