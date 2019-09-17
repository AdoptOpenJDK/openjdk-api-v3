package net.adoptopenjdk.api

import net.adoptopenjdk.api.v3.dataSources.github.VersionParser
import net.adoptopenjdk.api.v3.models.VersionData
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.test.assertTrue


class VersionParserTest {

    val testData = mapOf(
            Pair("OpenJDK 8u212 GA Release", VersionData(8, 0, 212, "", null, 0, "", "8u212")),
            Pair("OpenJDK8U-jdk_x64_linux_8u222b10.tar.gz", VersionData(8, 0, 222, "", null, 10, "", "8u222b10"))
    )

    @TestFactory
    fun parsesVersionsCorrectly(): Stream<DynamicTest> {
        return testData
                .map { v ->
                    DynamicTest.dynamicTest(v.key) {
                        val parsed = VersionParser().parse(v.key)
                        assertTrue(v.value.equals(parsed), "Should be ${v.value.semver}, was ${parsed.semver}")
                    }
                }
                .stream()
    }
}