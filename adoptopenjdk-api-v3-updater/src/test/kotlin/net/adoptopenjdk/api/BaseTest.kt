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
import net.adoptopenjdk.api.v3.AdoptRepository
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleaseSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleasesSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary
import net.adoptopenjdk.api.v3.dataSources.http.HttpClient
import net.adoptopenjdk.api.v3.dataSources.http.UrlRequest
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.GithubId
import net.adoptopenjdk.api.v3.models.Release
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
        val LOGGER = LoggerFactory.getLogger(this::class.java)

        private var mongodExecutable: MongodExecutable? = null

        val adoptRepos = UpdaterJsonMapper.mapper.readValue(GZIPInputStream(BaseTest::class.java.classLoader.getResourceAsStream("example-data.json.gz")), AdoptRepos::class.java)

        @JvmStatic
        @BeforeAll
        fun startDb() {
            System.setProperty("GITHUB_TOKEN", "stub-token")
            startFongo()
            LOGGER.info("Done startup")
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
            System.setProperty("MONGO_DB", "mongodb://localhost:$port")

            mongodExecutable = starter.prepare(mongodConfig)
            mongodExecutable!!.start()

            LOGGER.info("FMongo started")
        }

        @JvmStatic
        @AfterAll
        fun closeMongo() {
            mongodExecutable!!.stop()
        }

        fun mockkHttpClient(): HttpClient {
            return object : HttpClient {
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

        fun mockRepo(): AdoptRepository {
            return MockRepository(adoptRepos!!)
        }

        fun MockRepository(adoptRepos: AdoptRepos): AdoptRepository {
            return object : AdoptRepository {
                override suspend fun getReleaseById(id: GithubId): List<Release>? {
                    return adoptRepos.allReleases.getReleases().filter {
                        it.id.startsWith(id.githubId)
                    }.toList()
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
    }
}
