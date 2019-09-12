package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import net.adoptopenjdk.api.v3.models.*
import org.junit.jupiter.api.DynamicTest
import java.util.stream.Stream


@QuarkusTest
class AssetsResourceVersionPathTest : AssetsPathTest() {


    fun getPath() = "/v3/assets/version"
    val JAVA8_212 = "8.0.212+4.1"
    val RANGE_11_12 = "[11.0.0,12.0.0]"
    val JAVA11 = "11.0.0+28.1"

    override fun <T> runFilterTest(filterParamName: String, values: Array<T>): Stream<DynamicTest> {

        return listOf(
                "[8.0.0,)",
                JAVA8_212,
                RANGE_11_12,
                JAVA11
        )
                .flatMap { versionRange ->
                    createTest(values, getPath() + "/" + versionRange, filterParamName, { element -> getExclusions(versionRange, element) })
                }
                .stream()
    }

    private fun <T> getExclusions(versionRange: String, element: T): Boolean {
        return versionRange.equals(JAVA8_212) && element == JvmImpl.openj9 ||
                versionRange.equals(JAVA8_212) && element == Architecture.aarch64 ||
                versionRange.equals(JAVA8_212) && element == Architecture.arm ||
                versionRange.equals(JAVA8_212) && element == HeapSize.large ||
                versionRange.equals(RANGE_11_12) && element == OperatingSystem.solaris ||
                versionRange.equals(RANGE_11_12) && element == Architecture.sparcv9 ||
                versionRange.equals(JAVA11) && element == Architecture.x32 ||
                versionRange.equals(JAVA11) && element == OperatingSystem.solaris ||
                versionRange.equals(JAVA11) && element == Architecture.sparcv9 ||
                versionRange.equals(JAVA8_212) && element == ImageType.testimage ||
                versionRange.equals(JAVA11) && element == ImageType.testimage
    }
}

