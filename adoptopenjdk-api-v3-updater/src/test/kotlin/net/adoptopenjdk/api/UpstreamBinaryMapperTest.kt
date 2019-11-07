package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.mapping.upstream.UpstreamBinaryMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class UpstreamBinaryMapperTest {

    @Test
    fun filtersOutDebugInfo() {

        val jdk = GHAsset(
                "OpenJDK8U-jdk_x64_linux_8u232b09.tar.gz",
                1L,
                "",
                1L,
                "2013-02-27T19:35:32Z")

        val assets = listOf(GHAsset(
                "OpenJDK8U-jdk_x64_linux_8u232b09-debuginfo.tar.gz",
                1L,
                "",
                1L,
                "2013-02-27T19:35:32Z"),
                jdk
        )

        runBlocking {
            val binaryList = UpstreamBinaryMapper.toBinaryList(assets)

            assert(binaryList.size == 1)
            assertEquals(jdk.name, binaryList.get(0).`package`!!.name)
        }

    }

}