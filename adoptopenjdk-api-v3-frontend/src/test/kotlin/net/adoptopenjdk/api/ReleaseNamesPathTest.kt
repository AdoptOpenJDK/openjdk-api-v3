package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@QuarkusTest
class ReleaseNamesPathTest : BaseTest() {
    companion object {
        @JvmStatic
        @BeforeAll
        fun before() {
            populateDb()
        }
    }

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

    @Test
    fun releaseVersions() {

        RestAssured.given()
            .`when`()
            .get("/v3/info/release_versions")
            .then()
            .statusCode(200)
    }

    @Test
    fun releaseVersionsPageSize() {

        RestAssured.given()
            .`when`()
            .get("/v3/info/release_versions?page_size=50")
            .then()
            .statusCode(200)
    }

    @Test
    fun releaseVersionsSortOrder() {

        RestAssured.given()
            .`when`()
            .get("/v3/info/release_versions?sort_order=ASC")
            .then()
            .statusCode(200)
    }
}
