package net.adoptopenjdk.api.v3.dataSources.github.graphql.clients

import io.aexp.nodes.graphql.GraphQLRequestEntity
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClient
/* ktlint-disable no-wildcard-imports */
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.*
/* ktlint-enable no-wildcard-imports */
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphQLGitHubRepositoryClient @Inject constructor(
    graphQLRequest: GraphQLRequest,
    updaterHtmlClient: UpdaterHtmlClient
) : GraphQLGitHubReleaseRequest(graphQLRequest, updaterHtmlClient) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    suspend fun getRepository(owner:String, repoName: String): GHRepository {
        val requestEntityBuilder = getReleasesRequest(owner, repoName)

        LOGGER.info("Getting repo $repoName")

        val releases = getAll(
            requestEntityBuilder,
            { request -> getAllAssets(request) },
            { it.repository!!.releases.pageInfo.hasNextPage },
            { it.repository!!.releases.pageInfo.endCursor },
            clazz = QueryData::class.java
        )

        LOGGER.info("Done getting $repoName")

        return GHRepository(GHReleases(releases, PageInfo(false, null)))
    }

    private suspend fun getAllAssets(request: QueryData): List<GHRelease> {
        if (request.repository == null) return listOf()

        // nested releases based on how we deserialise githubs data
        return request.repository.releases.releases
            .map { release ->
                if (release.releaseAssets.pageInfo.hasNextPage) {
                    getNextPage(release)
                } else {
                    release
                }
            }
    }

    private fun getReleasesRequest(owner:String, repoName: String): GraphQLRequestEntity.RequestBuilder {
        return request(
            """
                        query(${'$'}cursorPointer:String) { 
                            repository(owner:"$owner", name:"$repoName") { 
                                releases(first:50, after:${'$'}cursorPointer, orderBy: {field: CREATED_AT, direction: DESC}) {
                                    nodes {
                                        id,
                                        url,
                                        name, 
                                        publishedAt,
                                        updatedAt,
                                        isPrerelease,
                                        resourcePath,
                                        releaseAssets(first:50) {
                                            nodes {
                                                downloadCount,
                                                updatedAt,
                                                name,
                                                downloadUrl,
                                                size
                                            },
                                            pageInfo {
                                                hasNextPage,
                                                endCursor
                                            }
                                        }
                                    },
                                    pageInfo {
                                        hasNextPage,
                                        endCursor
                                    }
                                }
                            }
                            rateLimit {
                                cost,
                                remaining
                            }
                        }
                    """
        )
    }
}
