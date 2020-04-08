package net.adoptopenjdk.api.v3

import javax.ws.rs.ApplicationPath
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.core.Application
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.routes.AssetsResource
import net.adoptopenjdk.api.v3.routes.BinaryResource
import net.adoptopenjdk.api.v3.routes.V1Route
import net.adoptopenjdk.api.v3.routes.VersionResource
import net.adoptopenjdk.api.v3.routes.info.AvailableReleasesResource
import net.adoptopenjdk.api.v3.routes.info.PlatformsResource
import net.adoptopenjdk.api.v3.routes.info.ReleaseListResource
import net.adoptopenjdk.api.v3.routes.info.VariantsResource
import net.adoptopenjdk.api.v3.routes.stats.DownloadStatsResource
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.info.Info
import org.eclipse.microprofile.openapi.annotations.servers.Server

object ServerConfig {
    const val SERVER = "https://api.adoptopenjdk.net"
}

const val DESCRIPTION = "<li><strong>NOTICE:</strong> AdoptOpenJDK API v1 Has now been removed.</li><li><strong>NOTICE:</strong> AdoptOpenJDK API v2 Has now been deprecated.</li><li><strong>NOTICE:</strong> If you are using v2 please move to the v3 as soon as possible. Please raise any migration problems as an issue in the <a href=\"https://github.com/AdoptOpenJDK/openjdk-api-v3/issues/new\">v3 issue tracker</a>.</li><li><strong>NOTICE:</strong> For v2 docs please refer to <a href=\"https://api.adoptopenjdk.net/README\">https://api.adoptopenjdk.net/README</a>.</li>"

@OpenAPIDefinition(
        servers = [
            Server(url = ServerConfig.SERVER),
            Server(url = "https://staging-api.adoptopenjdk.net")
        ],
        info = Info(title = "v3", version = "3.0.0", description = DESCRIPTION))
@ApplicationPath("/")
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
                V1Route::class.java,
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
