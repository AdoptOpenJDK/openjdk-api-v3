package net.adoptopenjdk.api.v3.dataSources.github

import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRepository
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary
import net.adoptopenjdk.api.v3.dataSources.models.GithubId

interface GitHubApi {
    suspend fun getRepository(repoName: String): GHRepository
    suspend fun getRepositorySummary(repoName: String): GHRepositorySummary
    suspend fun getReleaseById(id: GithubId): GHRelease?
}