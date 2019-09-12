package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import net.adoptopenjdk.api.v3.models.*
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream


@QuarkusTest
abstract class AssetsPathTest : BaseTest() {

    @TestFactory
    fun filtersOs(): Stream<DynamicTest> {
        return runFilterTest("os", OperatingSystem.values().map { it.name })
    }

    @TestFactory
    fun filtersArchitecture(): Stream<DynamicTest> {
        return runFilterTest("architecture", Architecture.values().map { it.name })
    }

    @TestFactory
    fun filtersImageType(): Stream<DynamicTest> {
        return runFilterTest("image_type", ImageType.values().map { it.name })
    }

    @TestFactory
    fun filtersJvmImpl(): Stream<DynamicTest> {
        return runFilterTest("jvm_impl", JvmImpl.values().map { it.name })
    }

    @TestFactory
    fun filtersHeapSize(): Stream<DynamicTest> {
        return runFilterTest("heap_size", HeapSize.values().map { it.name })
    }

    fun runFilterTest(filterParamName: String, values: List<String>): Stream<DynamicTest> {
        return values
                .map { it.toLowerCase() }
                .map {
                    DynamicTest.dynamicTest("${getPath()}/8?${filterParamName}=${it}") {
                        given()
                                .`when`()
                                .get("${getPath()}/8?${filterParamName}=${it}")
                                .then()
                                .statusCode(200)
                                .body("binaries.${filterParamName}.flatten()", everyItem(`is`(it)))
                                .body("binaries.${filterParamName}.flatten().size()", greaterThan(0))
                    }
                }
                .stream()
    }

    @Test
    fun badVersion() {
        given()
                .`when`()
                .get("${getPath()}/2")
                .then()
                .statusCode(404)
    }


    abstract fun getPath(): String
}

