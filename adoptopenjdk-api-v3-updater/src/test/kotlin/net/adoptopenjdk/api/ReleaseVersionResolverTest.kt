package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.ReleaseVersionResolver
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import net.adoptopenjdk.api.v3.dataSources.UrlRequest
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.ReleaseType
import org.apache.http.HttpResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseVersionResolverTest : BaseTest() {

    @BeforeEach
    fun init() {
        setHttpClient()
    }

    fun getRepos(): AdoptRepos {
        return runBlocking {
            val repo = AdoptReposBuilder.build(APIDataStore.variants.versions)

            val releases = repo.allReleases.getReleases()
                .filter { it.version_data.major < 13 || it.version_data.major == 13 && it.release_type == ReleaseType.ea }
                .groupBy { it.version_data.major }
                .toMap()
                .map { FeatureRelease(it.key, Releases(it.value)) }

            AdoptRepos(releases)
        }
    }

    @Test
    fun isStoredToDb() {
        runBlocking {
            val info = ReleaseVersionResolver.formReleaseInfo(getRepos())
            ReleaseVersionResolver.updateDbVersion(getRepos())
            val version = ApiPersistenceFactory.get().getReleaseInfo()
            assertEquals(JsonMapper.mapper.writeValueAsString(info), JsonMapper.mapper.writeValueAsString(version!!))
        }
    }

    @Test
    fun availableVersionsIsCorrect() {
        check { releaseInfo ->
            releaseInfo.available_releases.contentEquals(arrayOf(8, 9, 10, 11, 12))
        }
    }

    @Test
    fun availableLtsIsCorrect() {
        check { releaseInfo ->
            releaseInfo.available_lts_releases.contentEquals(arrayOf(8, 11))
        }
    }

    @Test
    fun mostRecentLtsIsCorrect() {
        check { releaseInfo ->
            releaseInfo.most_recent_lts == 11
        }
    }

    @Test
    fun mostRecentFeatureReleaseIsCorrect() {
        check { releaseInfo ->
            releaseInfo.most_recent_feature_release == 12
        }
    }

    @Test
    fun mostRecentFeatureVersionIsCorrect() {
        check { releaseInfo ->
            releaseInfo.most_recent_feature_version == 13
        }
    }

    private fun check(matcher: (ReleaseInfo) -> Boolean) {
        runBlocking {
            val info = ReleaseVersionResolver.formReleaseInfo(getRepos())
            assertTrue(matcher(info))
        }
    }

    @Test
    fun tipVersionIsCorrect() {
        check { releaseInfo ->
            releaseInfo.tip_version == 15
        }
    }

    private fun setHttpClient() {
        UpdaterHtmlClientFactory.client = object : UpdaterHtmlClient {
            override suspend fun get(url: String): String? {
                return getMetadata(url)
            }

            fun getMetadata(url: String): String {
                return """
                        DEFAULT_VERSION_FEATURE=15
                        DEFAULT_VERSION_INTERIM=0
                        """.trimIndent()
            }

            override suspend fun getFullResponse(request: UrlRequest): HttpResponse? {
                return null
            }
        }
    }
}
