package net.adoptopenjdk.api.v3

import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executors

object HttpClientFactory {
    private var client: HttpClient = HttpClient
            .newBuilder()
            .executor(Executors.newFixedThreadPool(2))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build()


    private var nonRedirect: HttpClient = HttpClient
            .newBuilder()
            .executor(Executors.newFixedThreadPool(2))
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(30))
            .build()

    fun getHttpClient(): HttpClient {
        return client
    }

    fun getNonRedirectHttpClient(): HttpClient {
        return nonRedirect
    }

    fun setClient(client: HttpClient) {
        this.client = client
        this.nonRedirect = client
    }
}