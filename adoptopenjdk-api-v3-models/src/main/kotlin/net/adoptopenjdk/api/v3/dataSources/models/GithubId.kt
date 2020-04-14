package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class GithubId {
    val githubId: String

    @JsonCreator
    constructor(
        @JsonProperty("githubId")
        githubId: String
    ) {
        this.githubId = githubId.split("\\.")[0]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GithubId

        if (githubId != other.githubId) return false

        return true
    }

    override fun hashCode(): Int {
        return githubId.hashCode()
    }
}
