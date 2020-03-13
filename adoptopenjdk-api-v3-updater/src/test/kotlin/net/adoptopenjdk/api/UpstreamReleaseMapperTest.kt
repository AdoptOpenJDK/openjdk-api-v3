package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAssets
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.mapping.upstream.UpstreamReleaseMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UpstreamReleaseMapperTest {

    @Test
    fun parsedSourceHasCorrectSourceInfo() {
        runBlocking {
            val ghRelease = getRelease()

            val release = UpstreamReleaseMapper.toAdoptRelease(ghRelease)

            assertEquals(ghRelease.releaseAssets.assets.get(0).downloadUrl, release!!.source!!.link)
            assertEquals(ghRelease.releaseAssets.assets.get(0).name, release.source!!.name)
            assertEquals(ghRelease.releaseAssets.assets.get(0).size, release.source!!.size)
        }
    }


    @Test
    fun statsOnlyLooksAtBinaryAssets() {
        runBlocking {
            val ghRelease = getRelease()

            val release = UpstreamReleaseMapper.toAdoptRelease(ghRelease)

            assertEquals(1, release!!.download_count)
        }
    }

    private fun getRelease(): GHRelease {
        val source = GHAssets(listOf(
                GHAsset("OpenJDK8U-sources_8u232b09.tar.gz",
                        1, "", 1, "2013-02-27T19:35:32Z"),
                GHAsset("OpenJDK8U-sources_8u232b09.tar.gz.json",
                        1, "", 1, "2013-02-27T19:35:32Z"),
                GHAsset("OpenJDK8U-sources_8u232b09.tar.gz.sha256.txt",
                        1, "", 1, "2013-02-27T19:35:32Z")
        ),
                PageInfo(false, "")
        )

        val ghRelease = GHRelease("1", "OpenJDK 8u232 GA Release", true, true, "2013-02-27T19:35:32Z", "2013-02-27T19:35:32Z", source, "8", "a-url");
        return ghRelease
    }
}