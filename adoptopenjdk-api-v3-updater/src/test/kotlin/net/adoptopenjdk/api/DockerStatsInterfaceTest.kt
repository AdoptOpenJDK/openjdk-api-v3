package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.DefaultUpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import net.adoptopenjdk.api.v3.stats.DockerStatsInterface
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class DockerStatsInterfaceTest {
    companion object {
        @JvmStatic
        @BeforeAll
        @Override
        fun startDb() {
            UpdaterHtmlClientFactory.client = DefaultUpdaterHtmlClient()
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

