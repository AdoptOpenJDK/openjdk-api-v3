package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.stream.Stream

@QuarkusTest
@ExtendWith(value = [DbExtension::class])
class ReleaseNamesPathTest : AssetsPathTest() {

    @Test
    fun releaseNames() {
        RestAssured.given()
            .`when`()
            .get("/v3/info/release_names")
            .then()
            .statusCode(200)
    }

    @Test
    fun releaseNamesPageSize() {

        RestAssured.given()
            .`when`()
            .get("/v3/info/release_names?page_size=50")
            .then()
            .statusCode(200)
    }

    @Test
    fun releaseNamesSortOrder() {

        RestAssured.given()
            .`when`()
            .get("/v3/info/release_names?sort_order=DESC")
            .then()
            .statusCode(200)
    }

    override fun <T> runFilterTest(filterParamName: String, values: Array<T>): Stream<DynamicTest> {
        return values
            .map { value ->
                DynamicTest.dynamicTest(value.toString()) {
                    RestAssured.given()
                        .`when`()
                        .get("/v3/info/release_names?$filterParamName=$value")
                        .then()
                        .statusCode(200)
                }
            }
            .stream()
    }
}
