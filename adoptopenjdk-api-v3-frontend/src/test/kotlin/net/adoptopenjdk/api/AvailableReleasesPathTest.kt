package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.Test

@QuarkusTest
class AvailableReleasesPathTest : BaseTest() {
    @Test
    fun availableReleases() {

        RestAssured.given()
                .`when`()
                .get("/v3/info/available_releases")
                .then()
                .statusCode(200)
    }
}
