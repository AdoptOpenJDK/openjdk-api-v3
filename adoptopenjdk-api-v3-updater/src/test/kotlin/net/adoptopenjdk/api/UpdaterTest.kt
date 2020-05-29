package net.adoptopenjdk.api

import com.google.inject.Binder
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.AdoptRepository
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.http.HttpClient
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoApiPersistence
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoClient
import net.adoptopenjdk.api.v3.models.Platforms
import net.adoptopenjdk.api.v3.models.Variants
import net.adoptopenjdk.api.v3.stats.DockerStatsInterface
import net.adoptopenjdk.api.v3.stats.GithubDownloadStatsCalculator
import org.junit.jupiter.api.BeforeEach
import java.util.*

abstract class UpdaterTest : BaseTest() {
    lateinit var injector: Injector

    class TestModule : Module {
        override fun configure(binder: Binder) {
            val client = mockkHttpClient()
            binder
                .bind(HttpClient::class.java)
                .toInstance(client)

            binder
                .bind(MongoClient::class.java)
                .toInstance(MongoClient(UUID.randomUUID().toString()))

            binder
                .bind(AdoptRepository::class.java)
                .toInstance(mockRepo())

            val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
            binder
                .bind(Platforms::class.java)
                .toInstance(JsonMapper.mapper.readValue(platformData, Platforms::class.java))

            val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
            binder
                .bind(Variants::class.java)
                .toInstance(JsonMapper.mapper.readValue(variantData, Variants::class.java))

            binder
                .bind(ApiPersistence::class.java)
                .to(MongoApiPersistence::class.java)

            binder
                .bind(AdoptRepos::class.java)
                .toInstance(adoptRepos)
        }
    }

    @BeforeEach
    fun beforeEach() {
        this.injector = Guice
            .createInjector(TestModule())
        populateDb()
        LOGGER.info("Done startup")
    }

    fun getMongoClient() = injector.getInstance(MongoClient::class.java)!!
    fun getApiDataStore() = injector.getInstance(APIDataStore::class.java)!!
    fun getApiPersistence() = injector.getInstance(ApiPersistence::class.java)!!
    fun getAdoptRepos() = injector.getInstance(AdoptRepos::class.java)!!
    fun getDockerStatsInterface() = injector.getInstance(DockerStatsInterface::class.java)!!
    fun getGithubDownloadStatsCalculator() = injector.getInstance(GithubDownloadStatsCalculator::class.java)!!
    fun getVariants() = injector.getInstance(Variants::class.java)!!
    fun getAdoptReposBuilder() = injector.getInstance(AdoptReposBuilder::class.java)!!

    fun populateDb() {
        runBlocking {
            getApiPersistence().updateAllRepos(getAdoptRepos())
        }
    }
}
