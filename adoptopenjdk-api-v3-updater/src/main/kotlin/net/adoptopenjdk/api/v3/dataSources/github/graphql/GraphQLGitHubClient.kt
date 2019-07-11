package net.adoptopenjdk.api.v3.dataSources.github.graphql

import net.adoptopenjdk.api.v3.dataSources.github.GitHubApi
import net.adoptopenjdk.api.v3.dataSources.github.graphql.clients.GraphQLGitHubReleaseClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.clients.GraphQLGitHubRepositoryClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.clients.GraphQLGitHubSummaryClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.Repository
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.RepositorySummary
import net.adoptopenjdk.api.v3.models.Release


class GraphQLGitHubClient : GitHubApi {
    private val summaryClient = GraphQLGitHubSummaryClient()
    private val releaseClient = GraphQLGitHubReleaseClient()
    private val repositoryClientClient = GraphQLGitHubRepositoryClient()

    override suspend fun getRepositorySummary(repoName: String): RepositorySummary {
        return summaryClient.getRepositorySummary(repoName)
    }

    override suspend fun getReleaseById(id: String): Release? {
        return releaseClient.getReleaseById(id)
    }

    override suspend fun getRepository(repoName: String): Repository {
        return repositoryClientClient.getRepository(repoName)
    }


}