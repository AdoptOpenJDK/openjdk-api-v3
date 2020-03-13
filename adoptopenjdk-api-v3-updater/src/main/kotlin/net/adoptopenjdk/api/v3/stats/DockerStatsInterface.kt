package net.adoptopenjdk.api.v3.stats

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import org.slf4j.LoggerFactory
import javax.json.JsonObject


class DockerStatsInterface {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    private val downloadStatsUrl = "https://hub.docker.com/v2/repositories/adoptopenjdk/"
    private val officialStatsUrl = "https://hub.docker.com/v2/repositories/library/adoptopenjdk/"

    private var database: ApiPersistence = ApiPersistenceFactory.get()

    suspend fun updateDb() {
        try {
            val stats = mutableListOf<DockerDownloadStatsDbEntry>()

            stats.addAll(getDownloadStats())
            stats.add(pullOfficalStats())

            database.addDockerDownloadStatsEntries(stats)
        } catch (e: Exception) {
            LOGGER.error("Failed to fetch docker download stats", e)
            throw e
        }
    }


    private fun getDownloadStats(): List<DockerDownloadStatsDbEntry> {
        val now = TimeSource.now()

        return pullAllStats()
                .map {
                    DockerDownloadStatsDbEntry(now, it.getJsonNumber("pull_count").longValue(), it.getString("name"))
                }
    }

    private fun pullOfficalStats(): DockerDownloadStatsDbEntry {
        val result = getStatsForUrl(officialStatsUrl)
        val now = TimeSource.now()

        return DockerDownloadStatsDbEntry(now, result.getJsonNumber("pull_count").longValue(), "official")
    }

    private fun pullAllStats(): ArrayList<JsonObject> {
        var next: String? = downloadStatsUrl

        val results = ArrayList<JsonObject>()
        while (next != null) {
            val stats = getStatsForUrl(next)
            results.addAll(stats.getJsonArray("results").map { it as JsonObject })
            next = stats.getString("next", null)
        }
        return results
    }

    private fun getStatsForUrl(url: String): JsonObject {
        return runBlocking {
            val stats = UpdaterHtmlClientFactory.client.get(url)
            if (stats == null) {
                throw Exception("Stats not returned")
            }

            try {
                val json = UpdaterJsonMapper.mapper.readValue(stats, JsonObject::class.java)
                if (json == null) {
                    throw Exception("Failed to parse stats")
                }
                return@runBlocking json
            } catch (e: Exception) {
                throw Exception("Failed to parse stats", e)
            }

        }
    }

}