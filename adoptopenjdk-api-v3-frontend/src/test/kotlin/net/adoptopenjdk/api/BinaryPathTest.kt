package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.response.Response
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.HeapSize
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


@QuarkusTest
class BinaryPathTest : BaseTest() {

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

    val path = "/v3/binary"

    fun getLatestPath(
            featureVersion: Int,
            releaseType: ReleaseType,
            os: OperatingSystem,
            arch: Architecture,
            imageType: ImageType,
            jvmImpl: JvmImpl,
            heapSize: HeapSize,
            vendor: Vendor
    ): String {
        return "${path}/latest/${featureVersion}/${releaseType}/${os}/${arch}/${imageType}/${jvmImpl}/${heapSize}/${vendor}"
    }

    fun getVersionPath(
            releaseName: String,
            os: OperatingSystem,
            arch: Architecture,
            imageType: ImageType,
            jvmImpl: JvmImpl,
            heapSize: HeapSize,
            vendor: Vendor
    ): String {
        return "${path}/version/${releaseName}/${os}/${arch}/${imageType}/${jvmImpl}/${heapSize}/${vendor}"
    }

    @Test
    fun latestDoesRedirectToBinary() {
        val path = getLatestPath(8, ReleaseType.ga, OperatingSystem.linux, Architecture.x64, ImageType.jdk, JvmImpl.hotspot, HeapSize.normal, Vendor.adoptopenjdk)
        performRequest(path)
                .then()
                .statusCode(307)
                .header("Location", Matchers.startsWith("https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/"))
    }

    @Test
    fun noExistantLatestRequestGives404() {
        val path = getLatestPath(4, ReleaseType.ga, OperatingSystem.linux, Architecture.x64, ImageType.jdk, JvmImpl.hotspot, HeapSize.normal, Vendor.adoptopenjdk)
        performRequest(path)
                .then()
                .statusCode(404)
    }

    @Test
    fun nonExistantVersionRequestRedirects() {
        val path = getVersionPath("jdk8u212-b04", OperatingSystem.linux, Architecture.x64, ImageType.jdk, JvmImpl.hotspot, HeapSize.normal, Vendor.adoptopenjdk)
        performRequest(path)
                .then()
                .statusCode(307)
                .header("Location", Matchers.startsWith("https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u212-b04"))
    }

    @Test
    fun nonExistantVersionRequestGives404() {
        val path = getVersionPath("fooBar", OperatingSystem.linux, Architecture.x64, ImageType.jdk, JvmImpl.hotspot, HeapSize.normal, Vendor.adoptopenjdk)
        performRequest(path)
                .then()
                .statusCode(404)
    }

    private fun performRequest(path: String): Response {
        return RestAssured.given()
                .`when`()
                .redirects().follow(false)
                .get(path)
    }

}

