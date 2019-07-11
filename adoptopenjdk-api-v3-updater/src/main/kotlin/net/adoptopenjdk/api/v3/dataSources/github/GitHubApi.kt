package net.adoptopenjdk.api.v3.dataSources.github

import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.Repository
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.RepositorySummary
import net.adoptopenjdk.api.v3.models.Release
import java.time.LocalDateTime

interface GitHubApi {
    suspend fun getRepository(repoName: String): Repository
    suspend fun getRepositorySummary(repoName: String): RepositorySummary
    suspend fun getReleaseById(id: String): Release?
}
