package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.Test
import javax.ws.rs.core.Response

@QuarkusTest
class V1RouteTest : BaseTest() {
    @Test
    fun v1ReturnsGone() {
        listOf("/v1",
                "/v1/",
                "/v1/foo",
                "/v1/foo/bar")
                .forEach({ route ->
                    RestAssured.given()
                            .`when`()
                            .get(route)
                            .then()
                            .statusCode(Response.Status.GONE.statusCode)
                })
    }
}