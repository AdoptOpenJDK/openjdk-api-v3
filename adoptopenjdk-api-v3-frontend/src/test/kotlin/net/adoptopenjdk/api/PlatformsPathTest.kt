package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.Test

@QuarkusTest
class PlatformsPathTest : BaseTest() {
    @Test
    fun platforms() {

        RestAssured.given()
            .`when`()
            .get("/v3/info/platforms")
            .then()
            .statusCode(200)
    }
}
