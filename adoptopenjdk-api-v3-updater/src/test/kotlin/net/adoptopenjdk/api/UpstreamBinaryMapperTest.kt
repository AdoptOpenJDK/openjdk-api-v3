package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.mapping.upstream.UpstreamBinaryMapper
import net.adoptopenjdk.api.v3.models.ImageType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class UpstreamBinaryMapperTest {

    companion object {
        @JvmStatic
        @BeforeAll
        public fun setup() {
            BaseTest.startFongo()
            BaseTest.mockRepo()
        }
    }

    private fun getAssetList(names: List<String>): List<GHAsset> {
        return names.flatMap { name ->
            listOf(
                    GHAsset(
                            name,
                            1L,
                            "",
                            1L,
                            "2013-02-27T19:35:32Z"),
                    GHAsset(
                            name + ".sign",
                            1L,
                            "a-signature-link to ${name}",
                            1L,
                            "2013-02-27T19:35:32Z")
            )
        }
    }


    @Test
    fun filtersOutDebugInfo() {
        val assets = getAssetList(listOf(
                "OpenJDK8U-jdk_x64_linux_8u232b09.tar.gz",
                "OpenJDK8U-jdk_x64_linux_8u232b09-debuginfo.tar.gz"
        ))
        runBlocking {
            val binaryList = UpstreamBinaryMapper.toBinaryList(assets)

            assert(binaryList.size == 1)
            assertEquals(assets.get(0).name, binaryList.get(0).`package`!!.name)
        }
    }

    @Test
    fun correctlyClassifiesImageType() {

        val assets = getAssetList(listOf(
                "OpenJDK11U-x64_linux_11.0.3_7.tar.gz",
                "OpenJDK11U-x64_windows_11.0.3_6_ea.zip",
                "OpenJDK11U-testimage_x64_linux_11.0.5_10.tar.gz",
                "OpenJDK11U-jre_aarch64_linux_11.0.5_10.tar.gz",
                "OpenJDK11U-jdk_aarch64_linux_11.0.5_10-debuginfo.tar.gz",
                "OpenJDK11U-sources_11.0.5_10.tar.gz"
        ))

        runBlocking {
            val binaryList = UpstreamBinaryMapper.toBinaryList(assets)

            assertEquals(4, binaryList.size)
            assertEquals(ImageType.jdk, binaryList.get(0).image_type)
            assertEquals(ImageType.jdk, binaryList.get(1).image_type)
            assertEquals(ImageType.testimage, binaryList.get(2).image_type)
            assertEquals(ImageType.jre, binaryList.get(3).image_type)
        }

    }


    @Test
    fun addsSignatureLink() {

        val assets = getAssetList(listOf(
                "OpenJDK11U-x64_linux_11.0.3_7.tar.gz"
        ))

        runBlocking {
            val binaryList = UpstreamBinaryMapper.toBinaryList(assets)
            assertEquals("a-signature-link to OpenJDK11U-x64_linux_11.0.3_7.tar.gz", binaryList.get(0).`package`?.signature_link)
        }

    }
}