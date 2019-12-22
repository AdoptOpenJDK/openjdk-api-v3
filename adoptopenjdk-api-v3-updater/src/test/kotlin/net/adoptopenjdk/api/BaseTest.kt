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
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.AdoptRepository
import net.adoptopenjdk.api.v3.AdoptRepositoryFactory
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleaseSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleasesSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoClientFactory
import net.adoptopenjdk.api.v3.models.Release
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.zip.GZIPInputStream
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
abstract class BaseTest {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)


        fun mockkHttpClient(): HttpClient {

            val client = mockk<HttpClient>()
            val checksumResponse = mockk<HttpResponse<String>>()


            every { client.sendAsync(match({ it.uri().toString().endsWith("sha256.txt") }), any<HttpResponse.BodyHandler<String>>()) } returns CompletableFuture.completedFuture(checksumResponse)
            every { checksumResponse.statusCode() } returns 200
            every { checksumResponse.body() } returns "CAFE123 IAmAChecksum"

            every { client.sendAsync(match({ it.uri().toString().endsWith("json") }), any<HttpResponse.BodyHandler<String>>()) } answers { arg ->

                val regex = """.*openjdk([0-9]+).*""".toRegex()
                val majorVersion = regex.find(arg.invocation.args.get(0).toString())!!.destructured.component1().toInt()

                val metadataResponse = mockk<HttpResponse<String>>()
                every { metadataResponse.statusCode() } returns 404
                CompletableFuture.completedFuture(metadataResponse)
            }

            return client
        }


        private var mongodExecutable: MongodExecutable? = null

        @JvmStatic
        @BeforeAll
        fun startDb() {
            System.setProperty("GITHUB_TOKEN", "stub-token")
            HttpClientFactory.client = mockkHttpClient()

            startFongo()

            mockRepo()
            LOGGER.info("Done startup")

        }

        @JvmStatic
        fun mockRepo() {
            val adoptRepos = JsonMapper.mapper.readValue(GZIPInputStream(javaClass.classLoader.getResourceAsStream("example-data.json.gz")), AdoptRepos::class.java)

            AdoptRepositoryFactory.adoptRepository = MockRepository(adoptRepos!!)
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

            LOGGER.info("Mongo \"mongodb://localhost:${port}\"")
            System.setProperty("MONGO_DB", "mongodb://localhost:${port}")

            mongodExecutable = starter.prepare(mongodConfig)
            mongodExecutable!!.start()

            ApiPersistenceFactory.set(null)
            MongoClientFactory.set(null)
            LOGGER.info("FMongo started")

        }

        fun MockRepository(adoptRepos: AdoptRepos): AdoptRepository {
            return object : AdoptRepository {
                override suspend fun getReleaseById(id: String): Release? {
                    return adoptRepos.allReleases.getReleases().filter { it.id == id }.first()
                }

                override suspend fun getRelease(version: Int): FeatureRelease {
                    return adoptRepos.repos.get(version)!!
                }

                override suspend fun getSummary(version: Int): GHRepositorySummary {
                    return repoToSummary(adoptRepos.repos.get(version)!!)
                }

                protected fun repoToSummary(featureRelease: FeatureRelease): GHRepositorySummary {

                    val gHReleaseSummarys = featureRelease.releases.getReleases()
                            .map {
                                GHReleaseSummary(it.id,
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC")).format(it.timestamp),
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC")).format(it.updated_at))
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
        }
    }


    protected suspend fun getInitialRepo(): AdoptRepos {
        return AdoptReposBuilder.incrementalUpdate(AdoptReposBuilder.build(APIDataStore.variants.versions))
    }
}
