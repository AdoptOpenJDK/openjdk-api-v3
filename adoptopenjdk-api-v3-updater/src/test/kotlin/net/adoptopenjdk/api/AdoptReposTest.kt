package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.AdoptRepositoryFactory
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.models.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertTrue


class AdoptReposTest : BaseTest() {

    @Test
    fun repoEqualityCheckIsCorrect() {
        runBlocking {
            val time = LocalDateTime.now()
            val a = formFeatureRelease(time)
            val b = formFeatureRelease(time)

            assertTrue { a == b }
        }
    }

    private fun formFeatureRelease(time: LocalDateTime): FeatureRelease {
        return FeatureRelease(8, Releases(listOf(Release(
                "a",
                ReleaseType.ga,
                "b",
                "c",
                time,
                time,
                arrayOf(Binary(
                        Package("a",
                                "b",
                                1L,
                                "v",
                                "c"
                        ),
                        2L,
                        time,
                        "d",
                        Installer("a",
                                "b",
                                1L,
                                "v",
                                "c"),
                        HeapSize.normal,
                        OperatingSystem.linux,
                        Architecture.x64,
                        ImageType.jdk,
                        JvmImpl.hotspot
                )),
                2,
                Vendor.adoptopenjdk,
                VersionData(
                        1,
                        2,
                        3,
                        "a",
                        4,
                        5,
                        "c",
                        "d"
                )

        ))))
    }


}

