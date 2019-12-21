package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.stats.DockerStatsInterface
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executors


class DockerStatsInterfaceTest {
    companion object {
        @JvmStatic
        @BeforeAll
        @Override
        fun startDb() {

            HttpClientFactory.client = HttpClient
                    .newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(Duration.ofSeconds(10))
                    .executor(Executors.newFixedThreadPool(6))
                    .build()

            BaseTest.startFongo()
        }
    }

    @Test
    fun dbEntryIsCreated() {
        runBlocking {
            DockerStatsInterface().updateDb()

            val stats = ApiPersistenceFactory.get().getLatestAllDockerStats()
            Assert.assertTrue(stats.size > 0)
        }
    }
}

