package net.adoptopenjdk.api


import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import net.adoptopenjdk.api.APITestUtils.Companion.createPermutations
import net.adoptopenjdk.api.APITestUtils.Companion.names
import net.adoptopenjdk.api.APITestUtils.Companion.runTest
import net.adoptopenjdk.api.v3.models.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import javax.ws.rs.core.Response


@QuarkusTest
open class BinaryTest {

    @TestFactory
    fun testDynamicTestStream(): Stream<DynamicTest> {
        return runTest(createPermutations(listOf(
                names(ReleaseType.values()),
                names(OperatingSystem.values()),
                names(Architecture.values()),
                names(BinaryType.values()),
                names(JvmImpl.values()),
                names(HeapSize.values())
        ), 500), { params ->
            val releaseType = params.get(0)
            val os = params.get(1)
            val architecture = params.get(2)
            val type = params.get(3)
            val jvmImpl = params.get(4)
            val heapSize = params.get(5)

            given()
                    .redirects()
                    .follow(false)
                    .`when`()
                    .get("/v3/binary/${releaseType}/8/${os}/${architecture}/latest/${type}/${jvmImpl}/${heapSize}")
                    .then()
                    .statusCode(Response.Status.FOUND.statusCode)
        })
    }

}

