package net.adoptopenjdk.api.v3.dataSources.mongo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    //private val internalDbStore = InternalDbStoreFactory.get()

    suspend fun getUrl(url: String): String? {
        return getData(url)

        /*
        val cachedEntry = internalDbStore.getCachedWebpage(url)
        return if (cachedEntry == null) {
            getData(url)
        } else {
            //Do refresh in the background
            GlobalScope.launch(Dispatchers.IO) {
                getData(url)
            }
            cachedEntry.data
        }
         */
    }

    private suspend fun getData(url: String): String? {
        return withContext(Dispatchers.IO) {
            val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build()

            //Retry up to 10 times
            for (retryCount in 1..10) {
                try {
                    val data = HttpClientFactory.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).get()

                    if (data.statusCode() == 200 && data.body() != null) {
                        return@withContext data.body()
                    } else if (data.statusCode() == 404) {
                        return@withContext null
                    } else {
                        LOGGER.error("Url status code ${data.statusCode()} ${retryCount} ${url}")
                    }
                } catch (e: Exception) {
                    LOGGER.error("Failed to read data retrying ${retryCount} ${url}")
                    delay(1000)
                }
            }

            /*

            val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build()

            //Retry up to 10 times
            for (retryCount in 1..10) {
                try {
                    val data = HttpClientFactory.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).get()

                    if (data.statusCode() == 200 && data.body() != null) {
                        val body = data.body()
                        internalDbStore.putCachedWebpage(url, body)
                        return@withContext body
                    } else if (data.statusCode() == NOT_FOUND.code()) {
                        internalDbStore.putCachedWebpage(url, null)
                        return@withContext null
                    } else {
                        LOGGER.error("Url status code ${data.statusCode()} ${retryCount} ${url}")
                    }
                } catch (e: Exception) {
                    LOGGER.error("Failed to read data retrying ${retryCount} ${url}")
                    delay(1000)
                }
            }
            */
            return@withContext null
        }
    }
}