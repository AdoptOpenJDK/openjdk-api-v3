package net.adoptopenjdk.api.v3.stats

import io.vertx.core.json.JsonObject
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime

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
            val stats = mutableListOf<DockerDownloadStatsDbEntry>();

            stats.addAll(getDownloadStats())
            stats.add(pullOfficalStats())

            database.addDockerDownloadStatsEntries(stats)
        } catch (e: Exception) {
            LOGGER.error("Failed to fetch docker download stats", e)
        }
    }


    private fun getDownloadStats(): List<DockerDownloadStatsDbEntry> {
        val now = LocalDateTime.now()

        return pullAllStats()
                .map {
                    DockerDownloadStatsDbEntry(now, it.getLong("pull_count"), it.getString("name"))
                }

    }

    private fun pullOfficalStats(): DockerDownloadStatsDbEntry {
        val result = getStatsForUrl(officialStatsUrl)
        val now = LocalDateTime.now()

        return DockerDownloadStatsDbEntry(now, result.getLong("pull_count"), "official")
    }

    private fun pullAllStats(): ArrayList<JsonObject> {
        var next: String = downloadStatsUrl

        val results = ArrayList<JsonObject>()
        do {
            val stats = getStatsForUrl(next)

            if (stats.containsKey("next")) {
                next = stats.getString("next")
            }

            results.addAll(stats.getJsonArray("results").map { it as JsonObject })

        } while (stats.containsKey("next"))


        return results
    }

    private fun getStatsForUrl(url: String): JsonObject {
        val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build()
        val stats = HttpClientFactory.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).get()
        return JsonObject(stats.body())
    }

}