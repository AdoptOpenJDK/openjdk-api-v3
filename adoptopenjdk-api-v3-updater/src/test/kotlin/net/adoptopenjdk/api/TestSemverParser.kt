package net.adoptopenjdk.api

import net.adoptopenjdk.api.v3.mapping.adopt.SemVer
import net.adoptopenjdk.api.v3.mapping.adopt.SemVerParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestSemverParser {

    @Test
    fun `parser works`() {
        //1.0.0-alpha+001, 1.0.0+20130313144700, 1.0.0-beta+exp.sha.5114f85, 1.0.0+21AF26D3—-117B344092BD
        assertEquals(
            SemVer(1, 0, 0, "beta.foo", listOf("beta", "foo"), "exp.sha.5114f85", listOf("exp", "sha", "5114f85")),
            SemVerParser.parse("1.0.0-beta.foo+exp.sha.5114f85")
        )

        assertEquals(
            SemVer(1, 0, 0, "alpha", listOf("alpha"), "001", listOf("001")),
            SemVerParser.parse("1.0.0-alpha+001")
        )

        assertEquals(
            SemVer(1, 0, 0, null, listOf(), "20130313144700", listOf("20130313144700")),
            SemVerParser.parse("1.0.0+20130313144700")
        )

        assertEquals(
            SemVer(1, 0, 0, "alpha", listOf("alpha"), null, listOf()),
            SemVerParser.parse("1.0.0-alpha")
        )
    }
}
