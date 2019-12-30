package net.adoptopenjdk.api.v3

import java.net.http.HttpClient
import java.time.Duration

object HttpClientFactory {
    var client: HttpClient = HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    fun getHttpClient(): HttpClient {
        return client
    }
}