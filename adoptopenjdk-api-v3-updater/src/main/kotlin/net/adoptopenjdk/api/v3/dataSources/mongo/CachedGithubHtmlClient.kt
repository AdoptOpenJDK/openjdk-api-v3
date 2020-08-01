package net.adoptopenjdk.api.v3.dataSources.mongo

import kotlinx.coroutines.*
import net.adoptopenjdk.api.v3.dataSources.DefaultUpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import net.adoptopenjdk.api.v3.dataSources.UrlRequest
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

interface GithubHtmlClient {
    suspend fun getUrl(url: String): String?
}

object CachedGithubHtmlClient : GithubHtmlClient {
    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @JvmStatic
    private val backgroundHtmlDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val internalDbStore = InternalDbStoreFactory.get()

    // List of urls to be refreshed in the background
    private val workList = LinkedBlockingQueue<UrlRequest>()

    init {
        // Do refresh in the background
        GlobalScope.launch(backgroundHtmlDispatcher, block = cacheRefreshDaemonThread())
    }

    override suspend fun getUrl(url: String): String? {
        val cachedEntry = internalDbStore.getCachedWebpage(url)
        return if (cachedEntry == null) {
            get(UrlRequest(url))
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
                    return@async get(request)
                }.await()
            }
        }
    }

    private suspend fun get(request: UrlRequest): String? {
        // Retry up to 10 times
        for (retryCount in 1..10) {
            try {
                LOGGER.info("Getting  ${request.url} ${request.lastModified}")
                val response = UpdaterHtmlClientFactory.client.getFullResponse(request)

                if (response?.statusLine?.statusCode == 304) {
                    // asset has not updated
                    return null
                }

                val body = DefaultUpdaterHtmlClient.extractBody(response)

                val lastModified = response?.getFirstHeader("Last-Modified")?.value

                internalDbStore.putCachedWebpage(request.url, lastModified, body)
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
