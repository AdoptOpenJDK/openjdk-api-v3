package net.adoptopenjdk.api

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import net.adoptopenjdk.api.v3.dataSources.UrlRequest
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAssets
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.models.GithubId
import net.adoptopenjdk.api.v3.mapping.adopt.AdoptReleaseMapper
import net.adoptopenjdk.api.v3.models.ReleaseType
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicStatusLine
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.fail

class AdoptReleaseMapperTest : BaseTest() {

    val jdk = GHAsset(
            "OpenJDK8U-jre_x64_linux_hotspot-123244354325.tar.gz",
            1L,
            "",
            1L,
            "2013-02-27T19:35:32Z")

    val checksum = GHAsset(
            "OpenJDK8U-jre_x64_linux_hotspot-123244354325.tar.gz.sha256.txt",
            1L,
            "",
            1L,
            "2013-02-27T19:35:32Z")

    @Test
    fun ignoresUnparsableVersion() {
        runBlocking {
            val source = GHAssets(listOf(jdk), PageInfo(false, ""))

            val ghRelease = GHRelease(GithubId("1"), "OpenJDK 123244354325", true, true, "2013-02-27T19:35:32Z", "2013-02-27T19:35:32Z", source, "8", "a-url")

            try {
                AdoptReleaseMapper.toAdoptRelease(ghRelease)
                fail("Did not throw exception")
            } catch (e: Exception) {
                return@runBlocking
            }
        }
    }

    @Test
    fun statsIgnoresNonBinaryAssets() {
        runBlocking {

            val source = GHAssets(listOf(jdk, checksum), PageInfo(false, ""))

            val ghRelease = GHRelease(GithubId("1"), "jdk9u-2018-09-27-08-50", true, true, "2013-02-27T19:35:32Z", "2013-02-27T19:35:32Z", source, "8", "https://github.com/AdoptOpenJDK/openjdk9-binaries/releases/download/jdk9u-2018-09-27-08-50/OpenJDK9U-jre_aarch64_linux_hotspot_2018-09-27-08-50.tar.gz")

            val release = AdoptReleaseMapper.toAdoptRelease(ghRelease)

            assertEquals(1, release.first().download_count)
        }
    }

    @Test
    fun obaysReleaseTypeforBinaryRepos() {
        runBlocking {

            val source = GHAssets(listOf(jdk), PageInfo(false, ""))

            val ghRelease = GHRelease(GithubId("1"), "jdk9u-2018-09-27-08-50", true, true, "2013-02-27T19:35:32Z", "2013-02-27T19:35:32Z", source, "8", "https://github.com/AdoptOpenJDK/openjdk9-binaries/releases/download/jdk9u-2018-09-27-08-50/OpenJDK9U-jre_aarch64_linux_hotspot_2018-09-27-08-50.tar.gz")

            val release = AdoptReleaseMapper.toAdoptRelease(ghRelease)

            assertEquals(ReleaseType.ea, release.first().release_type)
        }
    }

    @Test
    fun copesWithMultipleVersionsInSingleRelease() {
        runBlocking {

            val source = GHAssets(listOf(
                    GHAsset(
                            "OpenJDK8U-jre_x64_linux_hotspot-1.tar.gz",
                            1L,
                            "",
                            1L,
                            "2013-02-27T19:35:32Z"),
                    GHAsset(
                            "OpenJDK8U-jre_x64_linux_hotspot-1.tar.gz.json",
                            1L,
                            "1",
                            1L,
                            "2013-02-27T19:35:32Z"),
                    GHAsset(
                            "OpenJDK8U-jre_x64_linux_hotspot-2.tar.gz",
                            1L,
                            "",
                            1L,
                            "2013-02-27T19:35:32Z"),
                    GHAsset(
                            "OpenJDK8U-jre_x64_linux_hotspot-2.tar.gz.json",
                            1L,
                            "2",
                            1L,
                            "2013-02-27T19:35:32Z"),
                    GHAsset(
                            "OpenJDK8U-jre_x64_linux_hotspot-3.tar.gz",
                            1L,
                            "",
                            2L,
                            "2013-02-27T19:35:32Z"),
                    GHAsset(
                            "OpenJDK8U-jre_x64_linux_hotspot-3.tar.gz.json",
                            1L,
                            "2",
                            1L,
                            "2013-02-27T19:35:32Z")

            ), PageInfo(false, ""))

            UpdaterHtmlClientFactory.client = object : UpdaterHtmlClient {
                override suspend fun get(url: String): String? {
                    return getMetadata(url)
                }

                fun getMetadata(url: String): String {
                    val opt = UUID.randomUUID()
                    return """
                        {
                            "WARNING": "THIS METADATA FILE IS STILL IN ALPHA DO NOT USE ME",
                            "os": "windows",
                            "arch": "x86-32",
                            "variant": "openj9",
                            "version": {
                                "minor": 0,
                                "security": 242,
                                "pre": null,
                                "adopt_build_number": 1,
                                "major": 8,
                                "version": "1.8.0_242-$opt-b0$url",
                                "semver": "8.0.242+$url.1.$opt",
                                "build": $url,
                                "opt": "$opt"
                            },
                            "scmRef": "",
                            "version_data": "jdk8u",
                            "binary_type": "jre",
                            "sha256": "dc755cf762c867d4c71b782b338d2dc1500b468ab01adbf88620b5ae55eef42a"
                        }
                    """.trimIndent()
                            .replace("\n", "")
                }

                override suspend fun getFullResponse(request: UrlRequest): HttpResponse? {

                    val metadataResponse = mockk<HttpResponse>()

                    val entity = mockk<HttpEntity>()
                    every { entity.content } returns getMetadata(request.url)?.byteInputStream()
                    every { metadataResponse.statusLine } returns BasicStatusLine(ProtocolVersion("", 1, 1), 200, "")
                    every { metadataResponse.entity } returns entity
                    every { metadataResponse.getFirstHeader("Last-Modified") } returns BasicHeader("Last-Modified", "")
                    return metadataResponse
                }
            }

            val ghRelease = GHRelease(GithubId("1"), "jdk9u-2018-09-27-08-50", true, true, "2013-02-27T19:35:32Z", "2013-02-27T19:35:32Z", source, "8", "https://github.com/AdoptOpenJDK/openjdk9-binaries/releases/download/jdk9u-2018-09-27-08-50/OpenJDK9U-jre_aarch64_linux_hotspot_2018-09-27-08-50.tar.gz")

            val release = AdoptReleaseMapper.toAdoptRelease(ghRelease)

            assertEquals(2, release.size)
            assertEquals(1, release[0].binaries.size)
            assertEquals(2, release[1].binaries.size)

            assertEquals(1, release[0].download_count)
            assertEquals(3, release[1].download_count)
        }
    }
}