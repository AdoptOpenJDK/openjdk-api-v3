package net.adoptopenjdk.api.v3

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.adoptopenjdk.api.v3.dataSources.github.graphql.GraphQLGitHubClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleasesSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepo
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.GithubId
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import net.adoptopenjdk.api.v3.mapping.adopt.AdoptReleaseMapper
import net.adoptopenjdk.api.v3.mapping.upstream.UpstreamReleaseMapper
import net.adoptopenjdk.api.v3.models.Release
import org.slf4j.LoggerFactory
import javax.inject.Inject

interface AdoptRepository {
    suspend fun getRelease(version: Int): FeatureRelease?
    suspend fun getSummary(version: Int): GHRepositorySummary
    suspend fun getReleaseById(id: GithubId): List<Release>?
}

class AdoptRepositoryImpl @Inject constructor(
    val upstreamReleaseMapper: UpstreamReleaseMapper,
    val adoptReleaseMapper: AdoptReleaseMapper,
    val client: GraphQLGitHubClient
) : AdoptRepository {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    fun getMapperForRepo(url: String): ReleaseMapper {
        if (url.matches(".*/openjdk\\d+-upstream-binaries/.*".toRegex())) {
            return upstreamReleaseMapper
        } else {
            return adoptReleaseMapper
        }
    }

    override suspend fun getReleaseById(githubId: GithubId): List<Release>? {
        val release = client.getReleaseById(githubId)
        return getMapperForRepo(release.url)
            .toAdoptRelease(release)
    }

    override suspend fun getRelease(version: Int): FeatureRelease {
        val repo = getDataForEachRepo(version, ::getRepository)
            .await()
            .filterNotNull()
            .map { AdoptRepo(it) }
        return FeatureRelease(version, repo)
    }

    override suspend fun getSummary(version: Int): GHRepositorySummary {
        val releaseSummaries = getDataForEachRepo(version, { repoName: String -> client.getRepositorySummary(repoName) })
            .await()
            .filterNotNull()
            .flatMap { it.releases.releases }
            .toList()
        return GHRepositorySummary(GHReleasesSummary(releaseSummaries, PageInfo(false, "")))
    }

    private suspend fun getRepository(repoName: String): List<Release> {
        return client
            .getRepository(repoName)
            .getReleases()
            .flatMap<GHRelease, Release> {
                try {
                    val releases = getMapperForRepo(it.url).toAdoptRelease(it)
                    if (releases != null) {
                        return@flatMap releases
                    }
                } catch (e: Exception) {
                }
                return@flatMap emptyList<Release>()
            }
    }

    private suspend fun <E> getDataForEachRepo(version: Int, getFun: suspend (String) -> E): Deferred<List<E?>> {
        LOGGER.info("getting $version")
        return GlobalScope.async {
            return@async listOf(
                getRepoDataAsync("openjdk$version-openj9-releases", getFun),
                getRepoDataAsync("openjdk$version-openj9-nightly", getFun),
                getRepoDataAsync("openjdk$version-nightly", getFun),
                getRepoDataAsync("openjdk$version-binaries", getFun),
                getRepoDataAsync("openjdk$version-upstream-binaries", getFun)
            )
                .map { repo -> repo.await() }
        }
    }

    private fun <E> getRepoDataAsync(repoName: String, getFun: suspend (String) -> E): Deferred<E?> {
        return GlobalScope.async {
            LOGGER.info("getting $repoName")
            val releases = getFun(repoName)
            LOGGER.info("Done getting $repoName")
            return@async releases
        }
    }
}
