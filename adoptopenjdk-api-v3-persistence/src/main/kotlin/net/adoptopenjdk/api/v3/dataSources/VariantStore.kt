package net.adoptopenjdk.api.v3.dataSources

import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.models.Platforms
import net.adoptopenjdk.api.v3.models.Variants

object VariantStore {

    val platforms: Platforms
    val variants: Variants

    init {
        val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
        platforms = JsonMapper.mapper.readValue(platformData, Platforms::class.java)

        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = JsonMapper.mapper.readValue(variantData, Variants::class.java)
    }
}
