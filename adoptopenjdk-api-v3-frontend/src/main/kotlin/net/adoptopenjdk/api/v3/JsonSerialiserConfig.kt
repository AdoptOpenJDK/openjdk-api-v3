package net.adoptopenjdk.api.v3

import javax.json.bind.Jsonb
import javax.json.bind.JsonbBuilder
import javax.json.bind.JsonbConfig
import javax.ws.rs.ext.ContextResolver
import javax.ws.rs.ext.Provider

@Provider
class JsonSerialiserConfig : ContextResolver<Jsonb> {
    private val jsonb = JsonbBuilder.create(JsonbConfig().withFormatting(true))
    override fun getContext(objectType: Class<*>?): Jsonb {
        return jsonb
    }
}