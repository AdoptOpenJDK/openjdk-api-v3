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
import net.adoptopenjdk.api.v3.AdoptRepositoryFactory
import net.adoptopenjdk.api.v3.dataSources.*
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoClientFactory
import org.jboss.weld.environment.se.Weld
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
abstract class MongoTest {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        private var mongodExecutable: MongodExecutable? = null

        @Inject
        lateinit var apiDataStore: APIDataStore

        @JvmStatic
        @BeforeAll
        fun startDb() {
            System.setProperty("GITHUB_TOKEN", "stub-token")
            UpdaterHtmlClientFactory.client = BaseTest.mockkHttpClient()
            startFongo()
            mockRepo()
            val context = Weld().initialize()
            apiDataStore = context.select(APIDataStore::class.java).get()
        }

        @JvmStatic
        fun mockRepo() {
            val adoptRepos = UpdaterJsonMapper.mapper.readValue(GZIPInputStream(BaseTest::class.java.classLoader.getResourceAsStream("example-data.json.gz")), AdoptRepos::class.java)

            AdoptRepositoryFactory.setAdoptRepository(BaseTest.mockRepository(adoptRepos!!))
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
                val repo = AdoptReposBuilder.build(VariantStore.variants.versions)
                // Reset connection
                ApiPersistenceFactory.set(null)
                ApiPersistenceFactory.get().updateAllRepos(repo, "")
                apiDataStore.loadDataFromDb(true)
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
        return AdoptReposBuilder.incrementalUpdate(AdoptReposBuilder.build(VariantStore.variants.versions))
    }
}
