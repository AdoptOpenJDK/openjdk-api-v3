package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAssets
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.mapping.adopt.AdoptReleaseMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertNull


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

            val release = AdoptReleaseMapper.toAdoptRelease(ghRelease)

            assertNull(release)
        }
    }
}