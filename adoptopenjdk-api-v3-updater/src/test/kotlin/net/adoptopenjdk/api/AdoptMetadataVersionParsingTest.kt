package net.adoptopenjdk.api

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.mongo.CachedGithubHtmlClient
import net.adoptopenjdk.api.v3.mapping.adopt.AdoptBinaryMapper
import net.adoptopenjdk.api.v3.mapping.adopt.AdoptReleaseMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdoptMetadataVersionParsingTest {

    @Test
    fun usesMetadataForVersion() {
        runBlocking {
            BaseTest.startFongo()

            val ghClient = mockk<CachedGithubHtmlClient>()
            coEvery { ghClient.getUrl(any()) } returns """
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
                                "version": "1.8.0_242-202001081700-b06",
                                "semver": "8.0.242+6.1.202001081700",
                                "build": 6,
                                "opt": "202001081700"
                            },
                            "scmRef": "",
                            "version_data": "jdk8u",
                            "binary_type": "jre",
                            "sha256": "dc755cf762c867d4c71b782b338d2dc1500b468ab01adbf88620b5ae55eef42a"
                        }
                    """.trimIndent()

            val json = String(this.javaClass.classLoader.getResourceAsStream("example-release.json").readAllBytes())
            val release = JsonMapper.mapper.readValue(json, GHRelease::class.java)
            val adoptRelease = AdoptReleaseMapper(ghClient, AdoptBinaryMapper(ghClient)).toAdoptRelease(release)

            assertEquals(242, adoptRelease.first().version_data.security)
        }
    }
}
