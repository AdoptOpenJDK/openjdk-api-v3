package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.SortMethod
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.routes.AssetsResource
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.jboss.weld.junit5.EnableWeld
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.stream.Stream
import javax.ws.rs.BadRequestException

@ExtendWith(value = [DbExtension::class])
@QuarkusTest
@EnableWeld
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssetsResourceReleaseNamePathTest : FrontendTest() {

    lateinit var apiDataStore: APIDataStore

    @BeforeAll
    fun setup(apiDataStore: ApiDataStoreStub) {
        this.apiDataStore = apiDataStore
    }

    @TestFactory
    fun filtersByReleaseNameCorrectly(): Stream<DynamicTest> {
        return Vendor
            .values()
            .flatMap { vendor ->
                apiDataStore
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
                                        .and()
                                        .body(object : TypeSafeMatcher<String>() {
                                            override fun describeTo(description: Description?) {
                                                description!!.appendText("json")
                                            }

                                            override fun matchesSafely(p0: String?): Boolean {
                                                val returnedRelease = JsonMapper.mapper.readValue(p0, Release::class.java)
                                                return returnedRelease.id == release.id
                                            }
                                        })
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
    fun `release with different vendor 404s`(apiDataStore: APIDataStore) {
        val releaseName = apiDataStore
            .getAdoptRepos()
            .allReleases
            .getReleases(ReleaseFilter(vendor = Vendor.openjdk), SortOrder.DESC, SortMethod.DEFAULT)
            .first()
            .release_name

        RestAssured.given()
            .`when`()
            .get("/v3/assets/release_name/adoptopenjdk/$releaseName")
            .then()
            .statusCode(404)
    }

    @Test
    fun `missing release_name 400s`() {
        assertThrows<BadRequestException> {
            AssetsResource(apiDataStore)
                .get(
                    Vendor.adoptopenjdk,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
        }
    }

    @Test
    fun `missing vendor 400s`() {
        assertThrows<BadRequestException> {
            AssetsResource(apiDataStore)
                .get(
                    null,
                    "foo",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
        }
    }
}
