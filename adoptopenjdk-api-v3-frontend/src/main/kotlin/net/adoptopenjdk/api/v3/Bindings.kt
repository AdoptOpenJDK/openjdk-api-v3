package net.adoptopenjdk.api.v3

import net.adoptopenjdk.api.v3.dataSources.http.AsyncHttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.http.DefaultHttpClient
import net.adoptopenjdk.api.v3.dataSources.http.HttpClient
import net.adoptopenjdk.api.v3.models.Platforms
import net.adoptopenjdk.api.v3.models.Variants
import org.apache.http.nio.client.HttpAsyncClient
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Default
import javax.enterprise.inject.Produces
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class VariantFactory {
    @Produces
    @Singleton
    fun get(): Variants {
        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        val variants = JsonMapper.mapper.readValue(variantData, Variants::class.java)
        return variants
    }
}

@Singleton
class PlatformsFactory {
    @Produces
    @Singleton
    fun get(): Platforms {
        val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
        val platforms = JsonMapper.mapper.readValue(platformData, Platforms::class.java)
        return platforms
    }
}

@ApplicationScoped
@Default
class HttpClientFactory {
    val redirect = AsyncHttpClientFactory.getNonRedirectHttpClient()
    val nonredirect = AsyncHttpClientFactory.getHttpClient()

    @Produces
    @Default
    @ApplicationScoped
    fun get(): HttpClient {
        return DefaultHttpClient(redirect, nonredirect)
    }
}

@Singleton
class AsyncHttpClientFactoryQ {
    val redirect = AsyncHttpClientFactory.getNonRedirectHttpClient()
    val nonredirect = AsyncHttpClientFactory.getHttpClient()

    @Produces
    @Singleton
    @Named("non-redirect")
    fun getNonRedirect(): HttpAsyncClient {
        return nonredirect
    }

    @Produces
    @Singleton
    @Named("redirect")
    fun get(): HttpAsyncClient {
        return redirect
    }
}
