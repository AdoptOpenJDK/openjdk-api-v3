package net.adoptopenjdk.api.v3.dataSources

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.github.GithubAuth
import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.concurrent.FutureCallback
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.net.URL
import java.nio.charset.Charset
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class UrlRequest(
    val url: String,
    val lastModified: String? = null
)

interface UpdaterHtmlClient {
    suspend fun get(url: String): String?
    suspend fun getFullResponse(request: UrlRequest): HttpResponse?
}

object UpdaterHtmlClientFactory {
    var client: UpdaterHtmlClient = DefaultUpdaterHtmlClient()
}

class DefaultUpdaterHtmlClient : UpdaterHtmlClient {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
        private val TOKEN: String? = GithubAuth.readToken()
        private const val REQUEST_TIMEOUT = 12000L

        fun extractBody(response: HttpResponse?): String? {
            if (response == null) {
                return null
            }
            val writer = StringWriter()
            IOUtils.copy(response.entity.content, writer, Charset.defaultCharset())
            return writer.toString()
        }
    }

    class ResponseHandler(
        val client: DefaultUpdaterHtmlClient,
        private val continuation: Continuation<HttpResponse>,
        val request: UrlRequest?
    ) : FutureCallback<HttpResponse> {
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
                        client.getData(UrlRequest(response.getFirstHeader("location").value, request?.lastModified), continuation)
                    }
                    response.statusLine.statusCode == 404 -> {
                        continuation.resumeWithException(NotFoundException())
                    }
                    response.statusLine.statusCode == 200 || response.statusLine.statusCode == 304 -> {
                        continuation.resume(response)
                    }
                    else -> {
                        continuation.resumeWithException(Exception("Unexpected response ${response.statusLine.statusCode}"))
                    }
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
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

    private fun getData(urlRequest: UrlRequest, continuation: Continuation<HttpResponse>) {
        try {
            val url = URL(urlRequest.url)
            val request = RequestBuilder
                    .get(url.toURI())
                    .setConfig(HttpClientFactory.REQUEST_CONFIG)
                    .build()

            if (urlRequest.lastModified != null) {
                request.addHeader("If-Modified-Since", urlRequest.lastModified)
            }

            if (url.host.endsWith("github.com") && TOKEN != null) {
                request.setHeader("Authorization", "token $TOKEN")
            }

            val client =
                    if (url.host.endsWith("github.com")) {
                        HttpClientFactory.getNonRedirectHttpClient()
                    } else {
                        HttpClientFactory.getHttpClient()
                    }

            client.execute(request, ResponseHandler(this, continuation, urlRequest))
        } catch (e: Exception) {
            continuation.resumeWith(Result.failure(e))
        }
    }

    override suspend fun getFullResponse(request: UrlRequest): HttpResponse? {
        // Retry up to 10 times
        for (retryCount in 1..10) {
            try {
                LOGGER.info("Getting ${request.url} ${request.lastModified}")
                val response: HttpResponse = withTimeout(REQUEST_TIMEOUT) {
                    suspendCoroutine<HttpResponse> { continuation ->
                        getData(request, continuation)
                    }
                }
                LOGGER.info("Got  ${request.url}")
                return response
            } catch (e: NotFoundException) {
                return null
            } catch (e: Exception) {
                LOGGER.error("Failed to read data retrying $retryCount ${request.url}", e)
                delay(1000)
            }
        }

        return null
    }

    override suspend fun get(url: String): String? {
        return extractBody(getFullResponse(UrlRequest(url)))
    }

    class NotFoundException : Throwable()
}