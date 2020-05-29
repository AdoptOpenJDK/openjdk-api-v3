package net.adoptopenjdk.api

import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.http.HttpClient
import net.adoptopenjdk.api.v3.dataSources.http.UrlRequest
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.dataSources.persitence.ReleaseInfoFactory
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Variant
import net.adoptopenjdk.api.v3.models.Variants
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import org.apache.http.HttpResponse
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseInfoFactoryTest : UpdaterTest() {

    @Test
    fun versionIsCorrect() {
        val releaseInfo = getReleaseInfo()

        assertTrue(arrayOf(8, 9, 10, 11, 12, 13).contentEquals(releaseInfo.available_releases))
        assertTrue(arrayOf(8, 11).contentEquals(releaseInfo.available_lts_releases))
        assertEquals(11, releaseInfo.most_recent_lts)
        assertEquals(13, releaseInfo.most_recent_feature_release)
        assertEquals(50, releaseInfo.most_recent_feature_version)
        assertEquals(15, releaseInfo.tip_version)
    }

    private fun getReleaseInfo(): ReleaseInfo {
        val httpClient = createHttpClient()
        val variants: Variants = createVariants()
        val repos = getAdoptRepos()
        val map = repos.repos.toMutableMap()

        val repo = AdoptRepos(listOf(
            FeatureRelease(50, Releases(listOf(
                Release("foo", ReleaseType.ea, "a", "foo",
                    ZonedDateTime.of(2010, 1, 1, 1, 1, 0, 0, TimeSource.ZONE),
                    ZonedDateTime.of(2010, 1, 1, 1, 1, 0, 0, TimeSource.ZONE),
                    emptyArray(), 2, Vendor.adoptopenjdk,
                    VersionData(50, 0, 242, "b", null, 4, "b", "8u242-b04_openj9-0.18.0-m1")
                )
            )
            )
            )
        )
        )
        map.putAll(repo.repos)

        val releaseInfo = ReleaseInfoFactory(httpClient).formReleaseInfo(
            AdoptRepos(map),
            variants
        )
        return releaseInfo
    }

    private fun createVariants(): Variants {
        return Variants(arrayOf(
            Variant("a", JvmImpl.hotspot, Vendor.adoptopenjdk, 8, true, "", "", false),
            Variant("b", JvmImpl.hotspot, Vendor.adoptopenjdk, 9, false, "", "", false),
            Variant("c", JvmImpl.hotspot, Vendor.adoptopenjdk, 10, false, "", "", false),
            Variant("d", JvmImpl.hotspot, Vendor.adoptopenjdk, 11, true, "", "", false),
            Variant("e", JvmImpl.hotspot, Vendor.adoptopenjdk, 50, true, "", "", false)
        )
        )
    }

    private fun createHttpClient(): HttpClient {
        return object : HttpClient {
            override suspend fun get(url: String): String? {
                return """
                                # Default version, product, and vendor information to use,
                                # unless overridden by configure
                                
                                DEFAULT_VERSION_FEATURE=15
                                DEFAULT_VERSION_INTERIM=0
                                DEFAULT_VERSION_UPDATE=0
                                DEFAULT_VERSION_PATCH=0
                                """.trimIndent()
            }

            override suspend fun getFullResponse(request: UrlRequest): HttpResponse? {
                TODO("Not yet implemented")
            }
        }
    }
}
