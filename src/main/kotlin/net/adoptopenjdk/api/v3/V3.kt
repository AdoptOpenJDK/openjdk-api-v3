package net.adoptopenjdk.api.v3

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.info.Info
import org.eclipse.microprofile.openapi.annotations.servers.Server
import javax.ws.rs.core.Application

@OpenAPIDefinition(
        servers = [
            Server(url = "http://api.adoptopenjdk.com")
        ], info = Info(title = "v3", version = "3.0.0-beta"))
class V3 : Application() {

}