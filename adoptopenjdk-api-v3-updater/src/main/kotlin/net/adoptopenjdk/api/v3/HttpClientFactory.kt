package net.adoptopenjdk.api.v3

import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.RedirectStrategy
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.NoConnectionReuseStrategy
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.nio.client.HttpAsyncClient
import org.apache.http.protocol.HttpContext

object HttpClientFactory {
    private val client: HttpAsyncClient
    private val noRedirect: HttpAsyncClient

    val REQUEST_CONFIG = RequestConfig
            .copy(RequestConfig.DEFAULT)
            .setConnectTimeout(5000)
            .setSocketTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .build()!!

    init {

        val client = HttpAsyncClients.custom()
                .setConnectionReuseStrategy(NoConnectionReuseStrategy())
                .disableCookieManagement()
                .setDefaultRequestConfig(REQUEST_CONFIG)
                .build()
        client.start()
        this.client = client

        val noRedirect = HttpAsyncClients.custom()
                .setRedirectStrategy(object : RedirectStrategy {
                    override fun getRedirect(p0: HttpRequest?, p1: HttpResponse?, p2: HttpContext?): HttpUriRequest? {
                        return null
                    }

                    override fun isRedirected(p0: HttpRequest?, p1: HttpResponse?, p2: HttpContext?): Boolean {
                        return false
                    }
                })
                .setConnectionReuseStrategy(NoConnectionReuseStrategy())
                .setDefaultRequestConfig(REQUEST_CONFIG)
                .build()
        noRedirect.start()
        this.noRedirect = noRedirect
    }

    fun getHttpClient(): HttpAsyncClient {
        return client
    }

    fun getNonRedirectHttpClient(): HttpAsyncClient {
        return noRedirect
    }
}
