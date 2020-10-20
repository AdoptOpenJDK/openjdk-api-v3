package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.SortMethod
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import java.util.stream.Stream

@ExtendWith(value = [DbExtension::class])
@QuarkusTest
class AssetsResourceReleaseNamePathTest : FrontendTest() {
    @TestFactory
    fun filtersByReleaseNameCorrectly(): Stream<DynamicTest> {
        return Vendor
            .values()
            .flatMap { vendor ->
                APIDataStore
                    .getAdoptRepos()
                    .allReleases
                    .getReleases(ReleaseFilter(vendor = vendor), SortOrder.DESC, SortMethod.DEFAULT)
                    .take(3)
                    .flatMap { release ->
                        ReleaseType
                            .values()
                            .map { "/v3/assets/release_name/$vendor/${release.release_name}" }
                            .map {
                                DynamicTest.dynamicTest(it) {
                                    RestAssured.given()
                                        .`when`()
                                        .get(it)
                                        .then()
                                        .statusCode(200)
                                }
                            }
                            .asSequence()
                    }
                    .asIterable()
            }
            .stream()
    }

    @Test
    fun `non-existent release name 404s`() {
        RestAssured.given()
            .`when`()
            .get("/v3/assets/release_name/adoptopenjdk/foo")
            .then()
            .statusCode(404)
    }

    @Test
    fun `release with different vendor 404s`() {
        val releaseName = APIDataStore
            .getAdoptRepos()
            .allReleases
            .getReleases(ReleaseFilter(vendor = Vendor.openjdk), SortOrder.DESC, SortMethod.DEFAULT)
            .first()
            .release_name

        RestAssured.given()
            .`when`()
            .get("/v3/assets/release_name/adoptopenjdk/${releaseName}")
            .then()
            .statusCode(404)
    }


}
