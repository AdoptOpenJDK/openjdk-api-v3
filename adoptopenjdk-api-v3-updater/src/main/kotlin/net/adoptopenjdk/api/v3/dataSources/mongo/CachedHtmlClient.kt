package net.adoptopenjdk.api.v3.dataSources.mongo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.adoptopenjdk.api.v3.HttpClientFactory
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class CachedHtmlClient {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    private val internalDbStore = InternalDbStoreFactory.get()

    suspend fun getUrl(url: String): String? {
        val metadata = internalDbStore.getCachedWebpage(url)
        return if (metadata == null) {
            getData(url)
        } else {
            //Do refresh in the background
            GlobalScope.launch(Dispatchers.IO) {
                getData(url)
            }
            metadata
        }
    }

    private suspend fun getData(url: String): String? {
        return withContext(Dispatchers.IO) {
            val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build()

            val data = HttpClientFactory.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).get()

            if (data.statusCode() == 200 && data.body() != null) {
                try {
                    val body = data.body()
                    internalDbStore.putCachedWebpage(url, body)
                    return@withContext body
                } catch (e: Exception) {
                    LOGGER.error("Failed to read data", e)
                }
            }
            return@withContext null
        }
    }
}