package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHMetaData
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHVersion
import net.adoptopenjdk.api.v3.mapping.adopt.AdoptBinaryMapper
import net.adoptopenjdk.api.v3.models.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class AdoptBinaryMapperTest {

    val jdk = GHAsset(
            "OpenJDK8U-jre_x64_linux_hotspot-jfr_2019-11-21-10-26.tar.gz",
            1L,
            "",
            1L,
            "2013-02-27T19:35:32Z")

    val assets = listOf(jdk, GHAsset(
            "OpenJDK8U-jdk_x64_linux_hotspot_2019-11-22-16-01.tar.gz",
            1L,
            "",
            1L,
            "2013-02-27T19:35:32Z"))

    @Test
    fun parsesJfrFromName() {
        runBlocking {
            val binaryList = AdoptBinaryMapper.toBinaryList(assets, emptyMap())
            assertParsedHotspotJfr(binaryList)
        }
    }

    @Test
    fun projectDefaultsToJdk() {
        runBlocking {
            val binaryList = AdoptBinaryMapper.toBinaryList(assets, emptyMap())
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
            val binaryList = AdoptBinaryMapper.toBinaryList(assets, mapOf(Pair(jdk, metadata)))
            assertParsedHotspotJfr(binaryList)
        }
    }

    private fun assertParsedHotspotJfr(binaryList: List<Binary>) {
        assertEquals(JvmImpl.hotspot, binaryList.get(0).jvm_impl)
        assertEquals(Project.jfr, binaryList.get(0).project)
    }
}