package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoClient
import org.junit.jupiter.api.BeforeEach
import java.util.*
import javax.inject.Inject

@QuarkusTest
open class FrontEndTest : BaseTest() {

    @Inject
    lateinit var apiPersistence: ApiPersistence

    @Inject
    lateinit var mongoClient: MongoClient

    @BeforeEach
    fun disableUpdates() {
        System.setProperty("DISABLE_UPDATE", "true")
    }

    @BeforeEach
    fun populateDb() {
        mongoClient.connect(UUID.randomUUID().toString())
        runBlocking {
            apiPersistence.updateAllRepos(adoptRepos)
        }
    }
}
