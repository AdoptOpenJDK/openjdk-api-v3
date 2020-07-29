package net.adoptopenjdk.api.packages

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.response.Response
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.BaseTest
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.HeapSize
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.Project
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import org.junit.jupiter.api.BeforeAll

@QuarkusTest
abstract class PackageEndpointTest : BaseTest() {

    companion object {
        @JvmStatic
        @BeforeAll
        fun populateDb() {
            runBlocking {
                val repo = AdoptReposBuilder.build(APIDataStore.variants.versions)
                // Reset connection
                ApiPersistenceFactory.set(null)
                ApiPersistenceFactory.get().updateAllRepos(repo, "")
                APIDataStore.loadDataFromDb(true)
            }
        }
    }

    abstract fun getPath(): String

    fun getLatestPath(
        featureVersion: Int,
        releaseType: ReleaseType,
        os: OperatingSystem,
        arch: Architecture,
        imageType: ImageType,
        jvmImpl: JvmImpl,
        heapSize: HeapSize,
        vendor: Vendor,
        project: Project
    ): String {
        return "${getPath()}/latest/$featureVersion/$releaseType/$os/$arch/$imageType/$jvmImpl/$heapSize/$vendor?project=$project"
    }

    fun getVersionPath(
        releaseName: String,
        os: OperatingSystem,
        arch: Architecture,
        imageType: ImageType,
        jvmImpl: JvmImpl,
        heapSize: HeapSize,
        vendor: Vendor,
        project: Project
    ): String {
        return "${getPath()}/version/$releaseName/$os/$arch/$imageType/$jvmImpl/$heapSize/$vendor?project=$project"
    }

    protected fun performRequest(path: String): Response {
        return RestAssured.given()
            .`when`()
            .redirects().follow(false)
            .get(path)
    }
}
