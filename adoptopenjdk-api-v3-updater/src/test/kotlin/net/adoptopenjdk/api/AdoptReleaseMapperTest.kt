package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAssets
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.mapping.adopt.AdoptReleaseMapper
import net.adoptopenjdk.api.v3.models.ReleaseType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail


class AdoptReleaseMapperTest {

    val jdk = GHAsset(
            "OpenJDK8U-jre_x64_linux_hotspot-123244354325.tar.gz",
            1L,
            "",
            1L,
            "2013-02-27T19:35:32Z")

    @Test
    fun ignoresUnparsableVersion() {
        runBlocking {

            val source = GHAssets(listOf(jdk), PageInfo(false, ""))

            val ghRelease = GHRelease("1", "OpenJDK 123244354325", true, true, "2013-02-27T19:35:32Z", "2013-02-27T19:35:32Z", source, "8", "a-url");

            try {
                AdoptReleaseMapper.toAdoptRelease(ghRelease)
                fail("Did not throw exception")
            } catch (e: Exception) {
                return@runBlocking
            }

        }
    }


    @Test
    fun obaysReleaseTypeforBinaryRepos() {
        runBlocking {

            val source = GHAssets(listOf(jdk), PageInfo(false, ""))

            val ghRelease = GHRelease("1", "jdk9u-2018-09-27-08-50", true, true, "2013-02-27T19:35:32Z", "2013-02-27T19:35:32Z", source, "8", "https://github.com/AdoptOpenJDK/openjdk9-binaries/releases/download/jdk9u-2018-09-27-08-50/OpenJDK9U-jre_aarch64_linux_hotspot_2018-09-27-08-50.tar.gz");

            val release = AdoptReleaseMapper.toAdoptRelease(ghRelease)

            assertEquals(ReleaseType.ea, release.release_type)
        }
    }
}