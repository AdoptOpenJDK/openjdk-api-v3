package net.adoptopenjdk.api.v3

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.adoptopenjdk.api.v3.dataSources.github.graphql.GraphQLGitHubClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleasesSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.RepositorySummary
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepo
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.models.Release
import org.slf4j.LoggerFactory

object AdoptRepositoryFactory {
    var adoptRepository: AdoptRepository = AdoptRepositoryImpl
}

interface AdoptRepository {
    suspend fun getRelease(version: Int): FeatureRelease
    suspend fun getSummary(version: Int): RepositorySummary
    suspend fun getReleaseById(id: String): Release?
}

object AdoptRepositoryImpl : AdoptRepository {
    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    val client = GraphQLGitHubClient()

    override suspend fun getReleaseById(id: String): Release? {
        return client.getReleaseById(id)
    }

    override suspend fun getRelease(version: Int): FeatureRelease {
        val repo = getDataForEachRepo(version, ::getRepository)
                .await()
                .filterNotNull()
                .map { AdoptRepo(it) }
        return FeatureRelease(version, repo)
    }

    override suspend fun getSummary(version: Int): RepositorySummary {
        val releaseSummaries = getDataForEachRepo(version, { client.getRepositorySummary(it) })
                .await()
                .filterNotNull()
                .flatMap { it.releases.releases }
                .toList()
        return RepositorySummary(GHReleasesSummary(releaseSummaries, PageInfo(false, "")))

    }

    private suspend fun getRepository(repoName: String): List<Release> {
        return client.getRepository(repoName).getReleases()
    }

    private suspend fun <E> getDataForEachRepo(version: Int, getFun: suspend (String) -> E): Deferred<List<E?>> {
        LOGGER.info("getting $version")
        return GlobalScope.async {
            return@async listOf(
                    getRepoDataAsync("openjdk$version-openj9-nightly", getFun),
                    getRepoDataAsync("openjdk$version-nightly", getFun),
                    getRepoDataAsync("openjdk$version-binaries", getFun))
                    .map { repo -> repo.await() }
        }
    }

    private fun <E> getRepoDataAsync(repoName: String, getFun: suspend (String) -> E): Deferred<E?> {
        return GlobalScope.async {
            LOGGER.info("getting $repoName")
            try {
                val releases = getFun(repoName)
                LOGGER.info("Done getting $repoName")
                return@async releases
            } catch (e: Exception) {
                LOGGER.error("Failed when fetching ${repoName}", e)
                return@async null
            }
        }
    }
}