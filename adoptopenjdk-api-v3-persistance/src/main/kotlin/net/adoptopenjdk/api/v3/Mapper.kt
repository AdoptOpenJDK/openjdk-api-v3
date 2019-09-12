package net.adoptopenjdk.api.v3

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

object JsonMapper {
    val mapper = ObjectMapper()
            .findAndRegisterModules()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())
}