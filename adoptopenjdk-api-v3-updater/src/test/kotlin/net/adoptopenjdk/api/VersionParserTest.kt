package net.adoptopenjdk.api

import net.adoptopenjdk.api.v3.dataSources.github.VersionParser
import net.adoptopenjdk.api.v3.models.VersionData
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.test.assertTrue


class VersionParserTest {

    data class VersionTestData(val version: VersionData, val semver: String)

    val testData = mapOf(
            Pair("OpenJDK 8u212 GA Release",
                    VersionTestData(
                            VersionData(8, 0, 212, "", null, 0, "", "8u212"),
                            "8.0.212"
                    )
            ),

            Pair("OpenJDK8U-jdk_x64_linux_8u222b10.tar.gz",
                    VersionTestData(
                            VersionData(8, 0, 222, "", null, 10, "", "8u222b10"),
                            "8.0.222+10"
                    )
            ),
            Pair("jdk8u222-b10",
                    VersionTestData(
                            VersionData(8, 0, 222, "", null, 10, "", "8u222-b10"),
                            "8.0.222+10"
                    )
            ),
            Pair("jdk-9.0.4+11",
                    VersionTestData(
                            VersionData(9, 0, 4, "", null, 11, "", "9.0.4+11"),
                            "9.0.4+11"
                    )
            ),

            Pair("jdk-10.0.2+13.1",
                    VersionTestData(
                            VersionData(10, 0, 2, "", 1, 13, "", "10.0.2+13.1"),
                            "10.0.2+13.1"
                    )
            ),
            Pair("jdk-11.0.4+11.4",
                    VersionTestData(
                            VersionData(11, 0, 4, "", 4, 11, "", "11.0.4+11.4"),
                            "11.0.4+11.4"
                    )
            ),

            Pair("jdk-13+33_openj9-0.16.0",
                    VersionTestData(
                            VersionData(13, 0, 0, "", null, 33, "", "13+33"),
                            "13.0.0+33"
                    )
            ),

            Pair("jdk-13+33",
                    VersionTestData(
                            VersionData(13, 0, 0, "", null, 33, "", "13+33"),
                            "13.0.0+33"
                    )
            )


    )

    @TestFactory
    fun parsesVersionsCorrectly(): Stream<DynamicTest> {
        return testData
                .map { v ->
                    DynamicTest.dynamicTest(v.key) {
                        val parsed = VersionParser().parse(v.key)
                        assertTrue(v.value.version.equals(parsed), "Should be ${v.value.version.semver}, was ${parsed.semver}")
                        assertTrue(v.value.semver.equals(parsed.semver), "Should be ${v.value.semver}, was ${parsed.semver}")
                    }
                }
                .stream()
    }
}