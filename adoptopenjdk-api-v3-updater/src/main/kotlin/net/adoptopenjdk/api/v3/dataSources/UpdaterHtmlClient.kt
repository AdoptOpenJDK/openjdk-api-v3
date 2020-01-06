package net.adoptopenjdk.api.v3.dataSources

import kotlinx.coroutines.delay
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.github.GithubAuth
import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.concurrent.FutureCallback
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.net.URL
import java.nio.charset.Charset
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


interface UpdaterHtmlClient {
    suspend fun get(url: String): String?
}

object UpdaterHtmlClientFactory {
    var client: UpdaterHtmlClient = DefaultUpdaterHtmlClient()
}

class DefaultUpdaterHtmlClient : UpdaterHtmlClient {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
        private val TOKEN: String = GithubAuth.readToken()
    }

    class ResponseHandler(val client: DefaultUpdaterHtmlClient, val continuation: Continuation<String>) : FutureCallback<HttpResponse> {
        override fun cancelled() {
            continuation.resumeWithException(Exception("cancelled"))
        }

        override fun completed(response: HttpResponse?) {
            try {
                when {
                    response == null -> {
                        continuation.resumeWithException(Exception("No response body"))
                    }
                    isARedirect(response) -> {
                        client.getData(URL(response.getFirstHeader("location").value), continuation)
                    }
                    response.statusLine.statusCode == 404 -> {
                        continuation.resumeWithException(NotFoundException())
                    }
                    response.statusLine.statusCode == 200 -> {
                        continuation.resume(extractBody(response))
                    }
                    else -> {
                        continuation.resumeWithException(Exception("Unexpected response ${response.statusLine.statusCode}"))
                    }
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }

        private fun extractBody(response: HttpResponse): String {
            val writer = StringWriter()
            IOUtils.copy(response.entity.content, writer, Charset.defaultCharset())
            return writer.toString()
        }

        private fun isARedirect(response: HttpResponse): Boolean {
            return response.statusLine.statusCode == 307 ||
                    response.statusLine.statusCode == 301 ||
                    response.statusLine.statusCode == 302 ||
                    response.statusLine.statusCode == 303
        }

        override fun failed(e: java.lang.Exception?) {
            if (e == null) {
                continuation.resumeWithException(Exception("Failed Uknown reason"))
            } else {
                continuation.resumeWithException(e)
            }
        }
    }

    private fun getData(url: URL, continuation: Continuation<String>) {
        try {
            val request = HttpGet(url.toURI())

            if (url.host.endsWith("github.com")) {
                request.setHeader("Authorization", "token $TOKEN")
            }

            val client =
                    if (url.host.endsWith("github.com")) {
                        HttpClientFactory.getNonRedirectHttpClient()
                    } else {
                        HttpClientFactory.getHttpClient()
                    }

            client.execute(request, ResponseHandler(this, continuation))
        } catch (e: Exception) {
            continuation.resumeWith(Result.failure(e))
        }
    }


    override suspend fun get(url: String): String? {
        //Retry up to 10 times
        for (retryCount in 1..10) {
            try {
                LOGGER.info("Getting $url")
                val body: String = suspendCoroutine { continuation ->
                    getData(URL(url), continuation)
                }
                LOGGER.info("Got $url")
                return body
            } catch (e: NotFoundException) {
                return null
            } catch (e: Exception) {
                LOGGER.error("Failed to read data retrying $retryCount $url", e)
                delay(1000)
            }
        }

        return null
    }

    class NotFoundException : Throwable()
}

