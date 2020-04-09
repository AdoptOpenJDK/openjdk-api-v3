package net.adoptopenjdk.api.v3.dataSources.github.graphql.models

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import net.adoptopenjdk.api.v3.dataSources.models.GithubId

object GithubIdDeserializer : JsonDeserializer<GithubId>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext?): GithubId {
        return GithubId(parser.valueAsString)
    }
}
