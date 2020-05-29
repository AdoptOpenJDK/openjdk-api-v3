package net.adoptopenjdk.api.v3

import com.google.inject.Binder
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.name.Names
import net.adoptopenjdk.api.v3.dataSources.http.AsyncHttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.http.DefaultHttpClient
import net.adoptopenjdk.api.v3.dataSources.http.HttpClient
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoApiPersistence
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoClient
import net.adoptopenjdk.api.v3.models.Platforms
import net.adoptopenjdk.api.v3.models.Variants
import org.apache.http.nio.client.HttpAsyncClient

object GuiceBinding {
    class ApiPersistenceModule : Module {
        override fun configure(binder: Binder) {
            binder.bind(MongoClient::class.java)
                .asEagerSingleton()

            binder
                .bind(ApiPersistence::class.java)
                .to(MongoApiPersistence::class.java)
                .asEagerSingleton()

            val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
            val platforms = JsonMapper.mapper.readValue(platformData, Platforms::class.java)
            binder.bind(Platforms::class.java).to(platforms)

            val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
            val variants = JsonMapper.mapper.readValue(variantData, Variants::class.java)
            binder.bind(Variants::class.java).to(variants)

            bindHttpClients(binder)
        }
    }

    fun bindHttpClients(binder: Binder) {
        val redirectHttpClient = AsyncHttpClientFactory.getHttpClient()
        val noRedirect = AsyncHttpClientFactory.getNonRedirectHttpClient()

        binder
            .bind(HttpAsyncClient::class.java)
            .annotatedWith(Names.named("redirect"))
            .toInstance(redirectHttpClient)

        binder
            .bind(HttpAsyncClient::class.java)
            .annotatedWith(Names.named("non-redirect"))
            .toInstance(noRedirect)

        binder
            .bind(HttpClient::class.java)
            .toInstance(DefaultHttpClient(noRedirect, redirectHttpClient))
    }

    fun getInjector(): Injector {
        return Guice.createInjector(ApiPersistenceModule())
    }
}
