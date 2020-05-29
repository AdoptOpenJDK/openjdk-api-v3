package net.adoptopenjdk.api

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHMetaData
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHVersion
import net.adoptopenjdk.api.v3.dataSources.mongo.CachedGithubHtmlClient
import net.adoptopenjdk.api.v3.mapping.adopt.AdoptBinaryMapper
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.Binary
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.Project
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdoptBinaryMapperTest {

    companion object {
        @JvmStatic
        @BeforeAll
        public fun setup() {
            BaseTest.startFongo()
        }
    }

    val jdk = GHAsset(
        "OpenJDK8U-jre_x64_linux_hotspot-jfr_2019-11-21-10-26.tar.gz",
        1L,
        "",
        1L,
        "2013-02-27T19:35:32Z"
    )

    val assets = listOf(jdk, GHAsset(
        "OpenJDK8U-jdk_x64_linux_hotspot_2019-11-22-16-01.tar.gz",
        1L,
        "",
        1L,
        "2013-02-27T19:35:32Z"
    )
    )

    @Test
    fun oldChecksumIsFound() {
        runBlocking {
            val assets = listOf(GHAsset(
                "OpenJDK9-OPENJ9_ppc64le_Linux_jdk-9.0.4.12_openj9-0.9.0.tar.gz",
                1L,
                "",
                1L,
                "2013-02-27T19:35:32Z"
            ),
                GHAsset(
                    "OpenJDK9-OPENJ9_ppc64le_Linux_jdk-9.0.4.12_openj9-0.9.0.sha256.txt",
                    1L,
                    "a-download-link",
                    1L,
                    "2013-02-27T19:35:32Z"
                )
            )
            val binaryList = adoptBinaryMapper().toBinaryList(assets, assets, emptyMap())

            assertEquals("a-download-link", binaryList.get(0).`package`.checksum_link)
        }
    }

    @Test
    fun parsesOldOpenj9() {
        runBlocking {
            val assets = listOf(GHAsset(
                "OpenJDK9-OPENJ9_ppc64le_Linux_jdk-9.0.4.12_openj9-0.9.0.tar.gz",
                1L,
                "",
                1L,
                "2013-02-27T19:35:32Z"
            )
            )
            val binaryList = adoptBinaryMapper().toBinaryList(assets, assets, emptyMap())

            assertEquals(JvmImpl.openj9, binaryList.get(0).jvm_impl)
            assertEquals(Architecture.ppc64le, binaryList.get(0).architecture)
            assertEquals(OperatingSystem.linux, binaryList.get(0).os)
            assertEquals(Project.jdk, binaryList.get(0).project)
        }
    }

    @Test
    fun parsesJfrFromName() {
        runBlocking {
            val binaryList = adoptBinaryMapper().toBinaryList(assets, assets, emptyMap())
            assertParsedHotspotJfr(binaryList)
        }
    }

    @Test
    fun projectDefaultsToJdk() {
        runBlocking {
            val binaryList = adoptBinaryMapper().toBinaryList(assets, assets, emptyMap())
            assertEquals(Project.jdk, binaryList.get(1).project)
        }
    }

    @Test
    fun parsesJfrFromMetadata() {

        runBlocking {
            val metadata = GHMetaData("", OperatingSystem.linux, Architecture.x64, "hotspot-jfr",
                GHVersion(0, 1, 2, "", 4, "", 6, "", ""),
                "",
                "",
                ImageType.jdk,
                ""
            )
            val binaryList = adoptBinaryMapper().toBinaryList(assets, assets, mapOf(Pair(jdk, metadata)))
            assertParsedHotspotJfr(binaryList)
        }
    }

    @Test
    fun checkSumLinkFoundWhenChecksumIsSplitFromReleaseGroup() {
        runBlocking {
            val asset = GHAsset(
                "OpenJDK9-OPENJ9_ppc64le_Linux_jdk-9.0.4.12_openj9-0.9.0.tar.gz",
                1L,
                "",
                1L,
                "2013-02-27T19:35:32Z"
            )

            val checksum = GHAsset(
                "OpenJDK9-OPENJ9_ppc64le_Linux_jdk-9.0.4.12_openj9-0.9.0.sha256.txt",
                1L,
                "a-download-link",
                1L,
                "2013-02-27T19:35:32Z"
            )

            val binaryList = adoptBinaryMapper().toBinaryList(listOf(asset), listOf(asset, checksum), emptyMap())

            assertEquals("a-download-link", binaryList.get(0).`package`.checksum_link)
        }
    }

    private fun adoptBinaryMapper(): AdoptBinaryMapper {
        val ghClient = mockk<CachedGithubHtmlClient>()
        return AdoptBinaryMapper(ghClient)
    }

    private fun assertParsedHotspotJfr(binaryList: List<Binary>) {
        assertEquals(JvmImpl.hotspot, binaryList.get(0).jvm_impl)
        assertEquals(Project.jfr, binaryList.get(0).project)
    }
}
