package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.models.Variants
import java.io.File
import java.util.zip.GZIPOutputStream

class TestResourceGenerator {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                val mock = BaseTest.mockkHttpClient()
                HttpClientFactory.client = mock

                val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
                val variants = JsonMapper.mapper.readValue(variantData, Variants::class.java)

                val repo = AdoptReposBuilder.build(variants.versions)

                File("adoptopenjdk-api-v3-updater/src/test/resources/example-data.json.gz").delete()

                GZIPOutputStream(File("adoptopenjdk-api-v3-updater/src/test/resources/example-data.json.gz").outputStream()).use { out ->
                    JsonMapper.mapper.writerWithDefaultPrettyPrinter().writeValues(out).write(repo)
                }
            }
        }
    }
}