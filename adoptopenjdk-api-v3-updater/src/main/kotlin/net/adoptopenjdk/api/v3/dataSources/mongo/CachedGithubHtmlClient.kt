package net.adoptopenjdk.api.v3.dataSources.mongo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.adoptopenjdk.api.v3.dataSources.github.GithubAuth
import net.adoptopenjdk.api.v3.dataSources.http.DefaultHttpClient
import net.adoptopenjdk.api.v3.dataSources.http.UrlRequest
import org.apache.http.nio.client.HttpAsyncClient
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CachedGithubHtmlClient @Inject constructor(
    @Named("non-redirect")
    nonRedirectHttpClient: HttpAsyncClient,
    @Named("redirect")
    redirectHttpClient: HttpAsyncClient
) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    private val httpClient: DefaultHttpClient
    private val backgroundHtmlDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val internalDbStore = InternalDbStoreFactory.get()

    // List of urls to be refreshed in the background
    private val workList = LinkedBlockingQueue<UrlRequest>()

    init {
        // Do refresh in the background
        GlobalScope.launch(backgroundHtmlDispatcher, block = cacheRefreshDaemonThread())
        val TOKEN: String? = GithubAuth.readToken()
        this.httpClient = DefaultHttpClient(nonRedirectHttpClient, redirectHttpClient, TOKEN)
    }

    suspend fun getUrl(url: String): String? {
        val cachedEntry = internalDbStore.getCachedWebpage(url)
        return if (cachedEntry == null) {
            getNonCached(UrlRequest(url))
        } else {
            LOGGER.info("Scheduling for refresh $url ${cachedEntry.lastModified} ${workList.size}")
            workList.offer(UrlRequest(url, cachedEntry.lastModified))
            cachedEntry.data
        }
    }

    private fun cacheRefreshDaemonThread(): suspend CoroutineScope.() -> Unit {
        return {
            while (true) {
                val request = workList.take()
                async {
                    LOGGER.info("Enqueuing ${request.url} ${request.lastModified} ${workList.size}")
                    return@async getNonCached(request)
                }.await()
            }
        }
    }

    suspend fun getNonCached(request: UrlRequest, updateCache: Boolean = true): String? {
        // Retry up to 10 times
        for (retryCount in 1..10) {
            try {
                LOGGER.info("Getting  ${request.url} ${request.lastModified}")
                val response = httpClient.getFullResponse(request)

                if (response?.statusLine?.statusCode == 304) {
                    // asset has not updated
                    return null
                }

                val body = DefaultHttpClient.extractBody(response)

                val lastModified = response?.getFirstHeader("Last-Modified")?.value

                if (updateCache) {
                    internalDbStore.putCachedWebpage(request.url, lastModified, body)
                }
                LOGGER.info("Got ${request.url}")
                return body
            } catch (e: Exception) {
                LOGGER.error("Failed to read data retrying $retryCount ${request.url}", e)
                delay(1000)
            }
        }

        return null
    }
}
