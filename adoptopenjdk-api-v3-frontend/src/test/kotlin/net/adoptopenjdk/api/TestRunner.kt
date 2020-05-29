package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import org.awaitility.Awaitility
import org.junit.Ignore
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@QuarkusTest
@Ignore("For manual execution")
class TestRunner : FrontEndTest() {

    @Test
    @Ignore("For manual execution")
    fun run() {
        Awaitility.await().atMost(Long.MAX_VALUE, TimeUnit.NANOSECONDS).until({ 4 === 5 })
    }
}
