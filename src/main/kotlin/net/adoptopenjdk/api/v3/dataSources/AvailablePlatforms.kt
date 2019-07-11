package net.adoptopenjdk.api.v3.dataSources

import com.fasterxml.jackson.databind.ObjectMapper
import net.adoptopenjdk.api.v3.models.Platforms
import net.adoptopenjdk.api.v3.models.Variants

class AvailablePlatforms {


    companion object {
        val platforms: Platforms
        val variants: Variants

        init {
            val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
            platforms = ObjectMapper().readValue<Platforms>(platformData, Platforms::class.java);


            val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
            variants = ObjectMapper().readValue<Variants>(variantData, Variants::class.java);

        }
    }

}