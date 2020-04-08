package net.adoptopenjdk.api

import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAssets
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.mapping.upstream.UpstreamReleaseMapper
import org.junit.jupiter.api.Test

class UpstreamReleaseMapperTest {

    @Test
    fun parsedSourceHasCorrectSourceInfo() {
        runBlocking {
            val source = GHAssets(listOf(
                    GHAsset("OpenJDK8U-sources_8u232b09.tar.gz",
                            1, "", 1, "2013-02-27T19:35:32Z")),
                    PageInfo(false, "")
            )

            val ghRelease = GHRelease("1", "OpenJDK 8u232 GA Release", true, true, "2013-02-27T19:35:32Z", "2013-02-27T19:35:32Z", source, "8", "a-url")

            val release = UpstreamReleaseMapper.toAdoptRelease(ghRelease)

            assertEquals(ghRelease.releaseAssets.assets.get(0).downloadUrl, release!!.source!!.link)
            assertEquals(ghRelease.releaseAssets.assets.get(0).name, release.source!!.name)
            assertEquals(ghRelease.releaseAssets.assets.get(0).size, release.source!!.size)
        }
    }
}
