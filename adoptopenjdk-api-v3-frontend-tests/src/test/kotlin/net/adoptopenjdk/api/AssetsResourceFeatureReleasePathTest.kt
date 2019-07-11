package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import net.adoptopenjdk.api.v3.models.ReleaseType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream


@QuarkusTest
class AssetsResourceFeatureReleasePathTest : AssetsPathTest() {


    override fun getPath() = "/v3/assets/feature_releases/ga"


    @TestFactory
    fun noFilter(): Stream<DynamicTest> {
        return (8..12)
                .flatMap { version ->
                    ReleaseType.values()
                            .map { "/v3/assets/feature_releases/${it}/${version}" }
                            .map {
                                DynamicTest.dynamicTest(it) {
                                    RestAssured.given()
                                            .`when`()
                                            .get(it)
                                            .then()
                                            .statusCode(200)
                                }
                            }
                }
                .stream()
    }

    @Test
    fun badReleaseType() {
        RestAssured.given()
                .`when`()
                .get("${getPath()}/foo/8")
                .then()
                .statusCode(404)
    }
}

