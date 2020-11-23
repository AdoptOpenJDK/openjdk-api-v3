package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(value = [DbExtension::class])
@QuarkusTest
class VariantsPathTest : FrontendTest() {
    @Test
    fun variants() {

        RestAssured.given()
            .`when`()
            .get("/v3/info/variants")
            .then()
            .statusCode(200)
    }
}
