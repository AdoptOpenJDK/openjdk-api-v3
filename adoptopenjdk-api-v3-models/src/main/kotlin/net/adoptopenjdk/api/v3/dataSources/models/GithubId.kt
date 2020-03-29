package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class GithubId {
    val githubId: String

    @JsonCreator
    constructor(
            @JsonProperty("githubId")
            githubId: String) {
        this.githubId = githubId.split("\\.")[0]
    }
}
