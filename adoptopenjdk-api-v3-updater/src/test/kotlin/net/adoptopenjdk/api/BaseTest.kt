package net.adoptopenjdk.api

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.AdoptRepository
import net.adoptopenjdk.api.v3.AdoptRepositoryFactory
import net.adoptopenjdk.api.v3.ReleaseResult
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import net.adoptopenjdk.api.v3.dataSources.UrlRequest
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleaseSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleasesSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.GitHubId
import net.adoptopenjdk.api.v3.dataSources.mongo.InternalDbStoreFactory
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicStatusLine
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter

@ExtendWith(MockKExtension::class)
abstract class BaseTest {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        @JvmStatic
        public val adoptRepos = TestResourceDouble.generate()

        private var mockHtmlClient: UpdaterHtmlClient? = null

        fun mockkHttpClient(): UpdaterHtmlClient {
            if (mockHtmlClient == null) {
                mockHtmlClient = object : UpdaterHtmlClient {
                    override suspend fun get(url: String): String? {
                        if (url.endsWith("sha256.txt")) {
                            return "CAFE123 IAmAChecksum"
                        }

                        return null
                    }

                    override suspend fun getFullResponse(request: UrlRequest): HttpResponse? {
                        val metadataResponse = mockk<HttpResponse>()

                        val entity = mockk<HttpEntity>()
                        every { entity.content } returns get(request.url)?.byteInputStream()
                        every { metadataResponse.statusLine } returns BasicStatusLine(ProtocolVersion("", 1, 1), 200, "")
                        every { metadataResponse.entity } returns entity
                        every { metadataResponse.getFirstHeader("Last-Modified") } returns BasicHeader("Last-Modified", "")
                        return metadataResponse
                    }
                }
            }
            return mockHtmlClient!!
        }

        @JvmStatic
        @BeforeAll
        fun startDb() {
            System.setProperty("GITHUB_TOKEN", "stub-token")
            UpdaterHtmlClientFactory.client = mockkHttpClient()
        }

        @JvmStatic
        @BeforeAll
        fun mockRepo() {
            val repo = mockRepository(adoptRepos!!)
            val persistance = InMemoryApiPersistence()

            runBlocking {
                persistance.updateAllRepos(adoptRepos!!, "")
            }

            ApiPersistenceFactory.set(persistance)
            AdoptRepositoryFactory.setAdoptRepository(repo)
            InternalDbStoreFactory.set(InMemoryInternalDbStore())
            APIDataStore.loadDataFromDb(true)
        }

        fun mockRepository(adoptRepos: AdoptRepos): AdoptRepository {
            return object : AdoptRepository {
                override suspend fun getReleaseById(id: GitHubId): ReleaseResult {
                    return ReleaseResult(
                        result = adoptRepos.allReleases.getReleases().filter {
                            it.id.startsWith(id.githubId)
                        }.toList()
                    )
                }

                override suspend fun getRelease(version: Int): FeatureRelease? {
                    return adoptRepos.repos.get(version)
                }

                override suspend fun getSummary(version: Int): GHRepositorySummary {
                    return repoToSummary(adoptRepos.repos.get(version)!!)
                }

                protected fun repoToSummary(featureRelease: FeatureRelease): GHRepositorySummary {

                    val gHReleaseSummarys = featureRelease.releases.getReleases()
                        .map {
                            GHReleaseSummary(
                                GitHubId(it.id),
                                DateTimeFormatter.ISO_INSTANT.format(it.timestamp),
                                DateTimeFormatter.ISO_INSTANT.format(it.updated_at)
                            )
                        }
                        .toList()

                    return GHRepositorySummary(GHReleasesSummary(gHReleaseSummarys, PageInfo(false, "")))
                }
            }
        }
    }

    @BeforeEach
    fun restartRepo() {
        mockRepo()
    }

    protected suspend fun getInitialRepo(): AdoptRepos {
        return AdoptReposBuilder.incrementalUpdate(AdoptReposBuilder.build(APIDataStore.variants.versions))
    }
}
