package net.adoptopenjdk.api

import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
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
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import net.adoptopenjdk.api.v3.dataSources.UrlRequest
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleaseSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleasesSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.GithubId
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoClientFactory
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicStatusLine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
abstract class BaseTest {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        fun mockkHttpClient(): UpdaterHtmlClient {
            return object : UpdaterHtmlClient {
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

        private var mongodExecutable: MongodExecutable? = null

        @JvmStatic
        @BeforeAll
        fun startDb() {
            System.setProperty("GITHUB_TOKEN", "stub-token")
            UpdaterHtmlClientFactory.client = mockkHttpClient()
            startFongo()
            mockRepo()
            LOGGER.info("Done startup")
        }

        @JvmStatic
        fun mockRepo() {
            val adoptRepos = UpdaterJsonMapper.mapper.readValue(GZIPInputStream(BaseTest::class.java.classLoader.getResourceAsStream("example-data.json.gz")), AdoptRepos::class.java)

            AdoptRepositoryFactory.setAdoptRepository(MockRepository(adoptRepos!!))
        }

        @JvmStatic
        fun startFongo() {
            val starter = MongodStarter.getDefaultInstance()

            val bindIp = "localhost"
            val port = Random.nextInt(10000, 16000)
            val mongodConfig = MongodConfigBuilder()
                .version(Version.V4_0_2)
                .net(Net(bindIp, port, Network.localhostIsIPv6()))
                .build()

            val mongodbTestConnectionString = "mongodb://$bindIp:$port"
            LOGGER.info("Mongo test connection string - $mongodbTestConnectionString")
            System.setProperty("MONGODB_TEST_CONNECTION_STRING", mongodbTestConnectionString)

            mongodExecutable = starter.prepare(mongodConfig)
            mongodExecutable!!.start()

            ApiPersistenceFactory.set(null)
            MongoClientFactory.set(null)
            LOGGER.info("FMongo started")
        }

        @JvmStatic
        fun populateDb() {
            runBlocking {
                val repo = AdoptReposBuilder.build(APIDataStore.variants.versions)
                // Reset connection
                ApiPersistenceFactory.set(null)
                ApiPersistenceFactory.get().updateAllRepos(repo, "")
                APIDataStore.loadDataFromDb(true)
            }
        }

        fun MockRepository(adoptRepos: AdoptRepos): AdoptRepository {
            return object : AdoptRepository {
                override suspend fun getReleaseById(id: GithubId): ReleaseResult {
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
                                GithubId(it.id),
                                DateTimeFormatter.ISO_INSTANT.format(it.timestamp),
                                DateTimeFormatter.ISO_INSTANT.format(it.updated_at)
                            )
                        }
                        .toList()

                    return GHRepositorySummary(GHReleasesSummary(gHReleaseSummarys, PageInfo(false, "")))
                }
            }
        }

        @JvmStatic
        @AfterAll
        fun closeMongo() {
            mongodExecutable!!.stop()
            ApiPersistenceFactory.set(null)
            MongoClientFactory.set(null)
        }
    }

    protected suspend fun getInitialRepo(): AdoptRepos {
        return AdoptReposBuilder.incrementalUpdate(AdoptReposBuilder.build(APIDataStore.variants.versions))
    }
}
