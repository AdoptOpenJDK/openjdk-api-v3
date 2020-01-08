package net.adoptopenjdk.api.v3.dataSources.mongo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClientFactory
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

object CachedGithubHtmlClient {
    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @JvmStatic
    private val backgroundHtmlDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val internalDbStore = InternalDbStoreFactory.get()

    //List of urls to be refreshed in the background
    private val workList = LinkedBlockingQueue<String>()

    init {
        //Do refresh in the background
        GlobalScope.launch(backgroundHtmlDispatcher, block = cacheRefreshDaemonThread())
    }

    suspend fun getUrl(url: String): String? {
        val cachedEntry = internalDbStore.getCachedWebpage(url)
        return if (cachedEntry == null) {
            get(url)
        } else {
            workList.offer(url)
            cachedEntry.data
        }
    }

    private fun cacheRefreshDaemonThread(): suspend CoroutineScope.() -> Unit {
        return {
            while (true) {
                val url = workList.take()
                async {
                    LOGGER.info("Enqueuing $url")
                    return@async get(url)
                }.await()
            }
        }
    }

    private suspend fun get(url: String): String? {
        //Retry up to 10 times
        for (retryCount in 1..10) {
            try {
                LOGGER.info("Getting $url")
                val body = UpdaterHtmlClientFactory.client.get(url)
                internalDbStore.putCachedWebpage(url, body)
                LOGGER.info("Got $url")
                return body
            } catch (e: Exception) {
                LOGGER.error("Failed to read data retrying $retryCount $url", e)
                delay(1000)
            }
        }

        return null
    }

}

