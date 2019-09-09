package net.adoptopenjdk.api.v3.dataSources.github.graphql.clients

import io.aexp.nodes.graphql.GraphQLRequestEntity
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAssets
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.ReleaseQueryData
import org.slf4j.LoggerFactory


open class GraphQLGitHubReleaseRequest : GraphQLGitHubInterface() {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    protected suspend fun getNextPage(release: GHRelease): GHRelease {

        val getMore = getMoreReleasesQuery(release.id)
        LOGGER.info("Getting release assets ${release.id}")
        val moreAssets = getAll(getMore,
                { asset ->
                    if (asset.assetNode == null) listOf()
                    else asset.assetNode.releaseAssets.assets
                },
                { it.assetNode!!.releaseAssets.pageInfo.hasNextPage },
                { it.assetNode!!.releaseAssets.pageInfo.endCursor },
                release.releaseAssets.pageInfo.endCursor,
                null, ReleaseQueryData::class.java)

        val assets = release.releaseAssets.assets.union(moreAssets)

        return GHRelease(release.id, release.name, release.isPrerelease, release.prerelease, release.publishedAt, release.updatedAt, GHAssets(assets.toList(), PageInfo(false, null)), release.resourcePath, release.url)
    }

    private fun getMoreReleasesQuery(releaseId: String): GraphQLRequestEntity.RequestBuilder {
        return request("""query(${'$'}cursorPointer:String) { 
                              node(id:"$releaseId") {
                                ... on Release {
                                    releaseAssets(first:50, after:${'$'}cursorPointer) {
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