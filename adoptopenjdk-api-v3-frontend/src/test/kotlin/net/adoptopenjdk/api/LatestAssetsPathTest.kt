package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


@QuarkusTest
class LatestAssetsPathTest : BaseTest() {

    companion object {
        @JvmStatic
        @BeforeAll
        fun populateDb() {
            runBlocking {
                val repo = AdoptReposBuilder.build(APIDataStore.variants.versions)
                //Reset connection
                ApiPersistenceFactory.set(null)
                ApiPersistenceFactory.get().updateAllRepos(repo)
                APIDataStore.loadDataFromDb()
            }
        }
    }

    fun getPath() = "/v3/assets/latest"

    @Test
    fun latestAssetsReturnsSaneList() {

        val body = RestAssured.given()
                .`when`()
                .get("${getPath()}/8/${JvmImpl.hotspot}")
                .body

        val binaryStr = body.prettyPrint()

        val binaries = JsonMapper.mapper.readValue(binaryStr, List::class.java) as List<Map<String, String>>

        assert(hasEntryFor(binaries, OperatingSystem.linux, ImageType.jdk, Architecture.x64))
        assert(hasEntryFor(binaries, OperatingSystem.linux, ImageType.jre, Architecture.x64))
        assert(hasEntryFor(binaries, OperatingSystem.windows, ImageType.jdk, Architecture.x64))
        assert(hasEntryFor(binaries, OperatingSystem.windows, ImageType.jdk, Architecture.x64))
    }

    private fun hasEntryFor(binaries: List<Map<String, String>>, os: OperatingSystem, imageType: ImageType, architecture: Architecture): Boolean {
        val hasEntry = binaries
                .filter({ binary ->
                    binary.get("os") == os.name &&
                            binary.get("image_type") == imageType.name &&
                            binary.get("architecture") == architecture.name

                })
                .count() > 0
        return hasEntry
    }

}

