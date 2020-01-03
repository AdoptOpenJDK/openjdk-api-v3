package net.adoptopenjdk.api.v3.dataSources.mongo

import io.netty.handler.codec.http.HttpResponseStatus.FOUND
import io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND
import io.netty.handler.codec.http.HttpResponseStatus.PERMANENT_REDIRECT
import io.netty.handler.codec.http.HttpResponseStatus.TEMPORARY_REDIRECT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.github.GithubAuth
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

object CachedGithubHtmlClient {
    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @JvmStatic
    private val backgroundHtmlDispatcher = Executors.newFixedThreadPool(10).asCoroutineDispatcher()

    private val internalDbStore = InternalDbStoreFactory.get()

    //List of urls to be refreshed in the background
    private val workList = LinkedBlockingQueue<String>()
    private val TOKEN: String = GithubAuth.readToken()

    init {
        //Do refresh in the background
        GlobalScope.launch(backgroundHtmlDispatcher, block = cacheRefreshDaemonThread())
    }

    private fun cacheRefreshDaemonThread(): suspend CoroutineScope.() -> Unit {
        return {
            while (true) {
                val url = workList.take()
                getData(url)
            }
        }
    }

    suspend fun getUrl(url: String): String? {
        val cachedEntry = internalDbStore.getCachedWebpage(url)
        return if (cachedEntry == null) {
            getData(url)
        } else {
            workList.offer(url)
            cachedEntry.data
        }
    }

    private suspend fun get(request: HttpRequest): String {
        return suspendCoroutine { continuation ->
            followRedirect(request, continuation)
        }
    }

    //When providing the auth header only the first request and not redirects should pass this header
    private fun followRedirect(request: HttpRequest, continuation: Continuation<String>) {
        try {
            val data = HttpClientFactory
                    .getNonRedirectHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .orTimeout(20, TimeUnit.SECONDS)

            data.handle { result, error ->
                when {
                    error != null -> {
                        LOGGER.error("Failed to read data")
                        continuation.resumeWith(Result.failure(error))
                    }
                    result.statusCode() == NOT_FOUND.code() -> {
                        continuation.resumeWith(Result.failure(NotFoundException()))
                    }
                    result.statusCode() == TEMPORARY_REDIRECT.code() || result.statusCode() == PERMANENT_REDIRECT.code() || result.statusCode() == FOUND.code() -> {
                        val location = result.headers().firstValue("location")
                        if (location != null && location.isPresent()) {
                            getDataFromRedirect(location.get(), continuation)
                        } else {
                            continuation.resumeWith(Result.failure(Exception("Failed to redirect")))
                        }
                    }
                    result.body() == null -> {
                        continuation.resumeWith(Result.failure(NoDataException()))
                    }
                    else -> {
                        continuation.resumeWith(Result.success(result.body()))
                    }
                }
            }
        } catch (e: Exception) {
            continuation.resumeWith(Result.failure(e))
        }
    }

    private fun getDataFromRedirect(url: String, continuation: Continuation<String>) {
        try {
            val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build()

            HttpClientFactory
                    .getHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .orTimeout(20, TimeUnit.SECONDS)
                    .handle { result, error ->
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
        } catch (e: Exception) {
            continuation.resumeWith(Result.failure(e))
        }

    }

    private suspend fun getData(url: String): String? {
        val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .setHeader("Authorization", "token $TOKEN")
                .build()

        //Retry up to 10 times
        for (retryCount in 1..10) {
            try {
                LOGGER.info("Getting $url")
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

