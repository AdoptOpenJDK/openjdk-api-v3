package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import org.awaitility.Awaitility
import org.junit.Ignore
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@QuarkusTest
@Ignore("For manual execution")
class TestRunner : BaseTest() {

    companion object {
        @JvmStatic
        @BeforeAll
        fun populateDb(apiPersistence: ApiPersistence) {
            runBlocking {
                val repo = AdoptReposTestDataGenerator.generate()
                // Reset connection
                apiPersistence.updateAllRepos(repo, "")
            }
        }
    }

    @Test
    @Ignore("For manual execution")
    fun run() {
        Awaitility.await().atMost(Long.MAX_VALUE, TimeUnit.NANOSECONDS).until({ 4 === 5 })
    }
}
