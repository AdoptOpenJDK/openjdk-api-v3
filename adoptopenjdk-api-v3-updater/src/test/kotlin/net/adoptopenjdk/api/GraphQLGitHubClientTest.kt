package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.github.graphql.GraphQLGitHubClient
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GraphQLGitHubClientTest {
    @Test
    fun releaseUpdateTimeLooksSane() {
        runBlocking {
            val client = GraphQLGitHubClient()
            val summary = client.getRepository("openjdk11-binaries");

            summary
                    .releases
                    .releases
                    .forEach {
                        val assetUpdatedAt = it
                                .releaseAssets
                                .assets
                                .map { ReleaseMapper.parseDate(it.updatedAt) }
                                .max()
                        val updatedAt = ReleaseMapper.parseDate(it.updatedAt)
                        assertTrue("$assetUpdatedAt ${updatedAt}", { updatedAt.isAfter(assetUpdatedAt) || updatedAt == assetUpdatedAt })
                    }
        }
    }

    @Test
    fun summaryUpdateTimeLooksSane() {
        runBlocking {
            val client = GraphQLGitHubClient()
            val summary = client.getRepositorySummary("openjdk11-binaries");

            summary
                    .releases
                    .releases
                    .forEach {
                        assertNotNull(it.releaseAssets.assets[0].updatedAt)
                        val updatedAt = ReleaseMapper.parseDate(it.releaseAssets.assets[0].updatedAt)
                        assertTrue("$updatedAt ${it.getUpdatedTime()}", { it.getUpdatedTime().isAfter(updatedAt) || it.getUpdatedTime() == updatedAt })
                    }
        }
    }
}