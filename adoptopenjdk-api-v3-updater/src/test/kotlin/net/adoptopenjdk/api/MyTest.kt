package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.V3Updater
import net.adoptopenjdk.api.v3.models.JvmImpl
import org.awaitility.Awaitility
import org.junit.Ignore
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@Ignore("For manual execution")
class MyTest : BaseTest() {

    @Test
    @Ignore("For manual execution")
    fun run() {
        println("MyTest Running")

        runBlocking {

            var repo = getInitialRepo()
            repo.repos.values.forEach {
                println(it.featureVersion)
            }

            JvmImpl.values().forEach { println(it) }
        }
    }
}