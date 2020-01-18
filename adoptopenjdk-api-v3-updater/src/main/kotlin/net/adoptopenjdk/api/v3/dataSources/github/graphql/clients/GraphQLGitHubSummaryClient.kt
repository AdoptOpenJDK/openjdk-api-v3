package net.adoptopenjdk.api.v3.dataSources.github.graphql.clients

import io.aexp.nodes.graphql.GraphQLRequestEntity
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.QuerySummaryData
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleaseSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleasesSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import org.slf4j.LoggerFactory


class GraphQLGitHubSummaryClient : GraphQLGitHubInterface() {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    suspend fun getRepositorySummary(repoName: String): GHRepositorySummary {
        val requestEntityBuilder = getReleaseSummary(repoName)

        LOGGER.info("Getting repo summary $repoName")

        val releases = getAll(requestEntityBuilder,
                { request -> getSummary(request) },
                { it.repository!!.releases.pageInfo.hasNextPage },
                { it.repository!!.releases.pageInfo.endCursor },
                clazz = QuerySummaryData::class.java)

        val fixedUpdateTimeReleases = fixUpdateTimes(releases)

        LOGGER.info("Done getting summary $repoName")

        return GHRepositorySummary(GHReleasesSummary(fixedUpdateTimeReleases, PageInfo(false, null)))
    }

    private fun fixUpdateTimes(releases: List<GHReleaseSummary>): List<GHReleaseSummary> {
        //Fix broken github updatedAt times
        return releases
                .map {
                    val assetUpdatedAt = ReleaseMapper.parseDate(it.releaseAssets.assets.last().updatedAt)
                    val releaseUpdatedAt = ReleaseMapper.parseDate(it.updatedAt)

                    if (assetUpdatedAt.isAfter(releaseUpdatedAt)) {
                        GHReleaseSummary(
                                it.id,
                                it.publishedAt,
                                it.releaseAssets.assets.last().updatedAt,
                                it.releaseAssets
                        )
                    } else {
                        it
                    }
                }
    }

    private fun getSummary(request: QuerySummaryData): List<GHReleaseSummary> {
        if (request.repository == null) return listOf()

        //nested releases based on how we deserialise githubs data
        return request.repository.releases.releases
    }

    private fun getReleaseSummary(repoName: String): GraphQLRequestEntity.RequestBuilder {
        //This assumes the last node is the most recently updated, this seems to be the case, but cant be sure on that
        return request("""
                        query(${'$'}cursorPointer:String) { 
                            repository(owner:"$OWNER", name:"$repoName") { 
                                releases(first:50, after:${'$'}cursorPointer, orderBy: {field: CREATED_AT, direction: DESC}) {
                                    nodes {
                                        id,
                                        publishedAt,
                                        updatedAt,
                                        releaseAssets(last:1) {
                                            nodes {
                                                updatedAt
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