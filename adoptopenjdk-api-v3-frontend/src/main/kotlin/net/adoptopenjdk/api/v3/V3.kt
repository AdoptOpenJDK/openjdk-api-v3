package net.adoptopenjdk.api.v3

import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.routes.AssetsResource
import net.adoptopenjdk.api.v3.routes.BinaryResource
import net.adoptopenjdk.api.v3.routes.VersionResource
import net.adoptopenjdk.api.v3.routes.info.AvailableReleasesResource
import net.adoptopenjdk.api.v3.routes.info.PlatformsResource
import net.adoptopenjdk.api.v3.routes.info.ReleaseListResource
import net.adoptopenjdk.api.v3.routes.info.VariantsResource
import net.adoptopenjdk.api.v3.routes.stats.DownloadStatsResource
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.info.Info
import org.eclipse.microprofile.openapi.annotations.servers.Server
import javax.ws.rs.ApplicationPath
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.core.Application

object ServerConfig {
    const val SERVER = "https://api.adoptopenjdk.net"
}


@OpenAPIDefinition(
        servers = [
            Server(url = ServerConfig.SERVER)
        ],
        info = Info(title = "v3", version = "3.0.0-beta"))
@ApplicationPath("/v3")
class V3 : Application() {

    private val resourceClasses: Set<Class<out Any>>
    private val cors: Set<Any>

    init {

        cors = setOf(object : ContainerResponseFilter {
            override fun filter(requestContext: ContainerRequestContext?, responseContext: ContainerResponseContext) {
                responseContext.getHeaders().add("Access-Control-Allow-Origin", "*")
            }
        })

        //Eagerly fetch repo from db on app startup
        APIDataStore.getAdoptRepos()

        resourceClasses = setOf(
                AssetsResource::class.java,
                BinaryResource::class.java,
                AvailableReleasesResource::class.java,
                PlatformsResource::class.java,
                ReleaseListResource::class.java,
                VariantsResource::class.java,
                VersionResource::class.java,
                DownloadStatsResource::class.java)
    }

    override fun getSingletons(): Set<Any> {
        return cors
    }

    override fun getClasses(): Set<Class<*>> {
        return resourceClasses
    }
}
