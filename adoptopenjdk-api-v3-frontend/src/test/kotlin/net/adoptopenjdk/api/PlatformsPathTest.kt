package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@QuarkusTest
@ExtendWith(value = [DbExtension::class])
class PlatformsPathTest : FrontendTest() {
    @Test
    fun platforms() {
        RestAssured.given()
            .`when`()
            .get("/v3/info/platforms")
            .then()
            .statusCode(200)
    }
}
