package net.adoptopenjdk.api

import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.AdoptRepository
import net.adoptopenjdk.api.v3.AdoptRepositoryFactory
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHAssetDateSummaries
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHAssetDateSummary
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
import java.time.ZoneId
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

            LOGGER.info("Mongo \"mongodb://localhost:${port}\"")
            System.setProperty("MONGO_DB", "mongodb://localhost:${port}")

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
                //Reset connection
                ApiPersistenceFactory.set(null)
                ApiPersistenceFactory.get().updateAllRepos(repo)
                APIDataStore.loadDataFromDb()
            }
        }

        fun MockRepository(adoptRepos: AdoptRepos): AdoptRepository {
            return object : AdoptRepository {
                override suspend fun getReleaseById(id: String): Release? {
                    return adoptRepos.allReleases.getReleases().filter { it.id == id }.first()
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
                                GHReleaseSummary(it.id,
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC")).format(it.timestamp),
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC")).format(it.updated_at),
                                        GHAssetDateSummaries(listOf(GHAssetDateSummary(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC")).format(it.updated_at)))))
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
