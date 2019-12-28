package net.adoptopenjdk.api.v3.dataSources.mongo

import io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.adoptopenjdk.api.v3.HttpClientFactory
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Executors
import kotlin.coroutines.suspendCoroutine

class CachedHtmlClient {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        val backgroundHtmlDispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()
    }

    private val internalDbStore = InternalDbStoreFactory.get()

    suspend fun getUrl(url: String): String? {
        val cachedEntry = internalDbStore.getCachedWebpage(url)
        return if (cachedEntry == null) {
            getData(url)
        } else {
            //Do refresh in the background
            GlobalScope.launch(backgroundHtmlDispatcher) {
                getData(url)
            }
            cachedEntry.data
        }
    }

    private suspend fun get(request: HttpRequest): String {
        return suspendCoroutine { continuation ->
            val data = HttpClientFactory.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())

            data.handle { result, error ->
                when {
                    error != null -> {
                        LOGGER.error("Failed to read data")
                        continuation.resumeWith(Result.failure(error))
                    }
                    result.statusCode() == NOT_FOUND.code() -> {
                        continuation.resumeWith(Result.failure(NotFoundException()))
                    }
                    result.body() == null -> {
                        continuation.resumeWith(Result.failure(NoDataException()))
                    }
                    else -> {
                        continuation.resumeWith(Result.success(result.body()))
                    }
                }
            }
        }
    }

    private suspend fun getData(url: String): String? {
        val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build()

        //Retry up to 10 times
        for (retryCount in 1..10) {
            try {
                val body = get(request)
                internalDbStore.putCachedWebpage(url, body)
                return body
            } catch (e: NotFoundException) {
                internalDbStore.putCachedWebpage(url, null)
                return null
            } catch (e: Exception) {
                LOGGER.error("Failed to read data retrying $retryCount $url")
                delay(1000)
            }
        }

        return null
    }

    class NotFoundException : Throwable()
    class NoDataException : Throwable()
}

