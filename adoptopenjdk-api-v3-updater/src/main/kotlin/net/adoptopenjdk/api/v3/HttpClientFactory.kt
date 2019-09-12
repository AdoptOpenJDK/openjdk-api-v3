package net.adoptopenjdk.api.v3

import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executors


object HttpClientFactory {
    // Current default impl is Graphql impl
    var client: HttpClient = HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newFixedThreadPool(6))
            .build()

    fun getHttpClient(): HttpClient {
        return client
    }
}