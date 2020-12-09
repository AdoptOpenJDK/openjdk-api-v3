package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.ReleaseVersionResolver
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import net.adoptopenjdk.api.v3.dataSources.UrlRequest
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import org.apache.http.HttpResponse
import org.jboss.weld.junit5.auto.AddPackages
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@AddPackages(value = [ReleaseVersionResolver::class])
class ReleaseVersionResolverTest : BaseTest() {

    private var releaseVersionResolver: ReleaseVersionResolver? = null

    @BeforeEach
    fun init(releaseVersionResolver: ReleaseVersionResolver) {
        setHttpClient()
        this.releaseVersionResolver = releaseVersionResolver
    }

    @Test
    fun isStoredToDb(apiPersistence: ApiPersistence) {
        runBlocking {
            val info = releaseVersionResolver!!.formReleaseInfo(adoptRepos)
            releaseVersionResolver!!.updateDbVersion(adoptRepos)
            val version = apiPersistence.getReleaseInfo()
            assertEquals(JsonMapper.mapper.writeValueAsString(info), JsonMapper.mapper.writeValueAsString(version!!))
        }
    }

    @Test
    fun availableVersionsIsCorrect() {
        check { releaseInfo ->
            releaseInfo.available_releases.contentEquals(AdoptReposTestDataGenerator.TEST_VERSIONS.toTypedArray())
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
            releaseInfo.most_recent_feature_version == 12
        }
    }

    private fun check(matcher: (ReleaseInfo) -> Boolean) {
        runBlocking {
            val info = releaseVersionResolver!!.formReleaseInfo(adoptRepos)
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
