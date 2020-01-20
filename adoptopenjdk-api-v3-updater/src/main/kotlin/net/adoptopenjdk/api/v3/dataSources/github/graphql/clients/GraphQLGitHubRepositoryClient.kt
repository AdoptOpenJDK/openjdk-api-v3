package net.adoptopenjdk.api.v3.dataSources.github.graphql.clients

import io.aexp.nodes.graphql.GraphQLRequestEntity
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHReleases
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRepository
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.QueryData
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import org.slf4j.LoggerFactory


open class GraphQLGitHubRepositoryClient : GraphQLGitHubReleaseRequest() {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        fun fixReleaseUpdateTime(it: GHRelease): GHRelease {
            val assetUpdatedAt = ReleaseMapper.parseDate(it.releaseAssets.assets.last().updatedAt)
            val releaseUpdatedAt = ReleaseMapper.parseDate(it.updatedAt)

            return if (assetUpdatedAt.isAfter(releaseUpdatedAt)) {
                GHRelease(
                        it.id,
                        it.name,
                        it.isPrerelease,
                        it.prerelease,
                        it.publishedAt,
                        it.releaseAssets.assets.last().updatedAt,
                        it.releaseAssets,
                        it.resourcePath,
                        it.url)
            } else {
                it
            }
        }


    }

    suspend fun getRepository(repoName: String): GHRepository {
        val requestEntityBuilder = getReleasesRequest(repoName)

        LOGGER.info("Getting repo $repoName")

        val releases = getAll(requestEntityBuilder,
                { request -> getAllAssets(request) },
                { it.repository!!.releases.pageInfo.hasNextPage },
                { it.repository!!.releases.pageInfo.endCursor },
                clazz = QueryData::class.java)

        LOGGER.info("Done getting $repoName")

        val fixed = fixUpdateTimes(releases)
        return GHRepository(GHReleases(fixed, PageInfo(false, null)))
    }

    fun fixUpdateTimes(releases: List<GHRelease>): List<GHRelease> {
        //Fix broken github updatedAt times
        return releases
                .map {
                    fixReleaseUpdateTime(it)
                }
    }

    private suspend fun getAllAssets(request: QueryData): List<GHRelease> {
        if (request.repository == null) return listOf()

        //nested releases based on how we deserialise githubs data
        return request.repository.releases.releases
                .map { release ->
                    if (release.releaseAssets.pageInfo.hasNextPage) {
                        getNextPage(release)
                    } else {
                        release
                    }
                }
    }

    private fun getReleasesRequest(repoName: String): GraphQLRequestEntity.RequestBuilder {
        return request("""
                        query(${'$'}cursorPointer:String) { 
                            repository(owner:"$OWNER", name:"$repoName") { 
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
                    """)
    }

}