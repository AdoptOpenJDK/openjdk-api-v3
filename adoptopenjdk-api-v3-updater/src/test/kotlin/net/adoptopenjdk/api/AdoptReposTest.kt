package net.adoptopenjdk.api

import java.time.ZonedDateTime
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.Binary
import net.adoptopenjdk.api.v3.models.HeapSize
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.Installer
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.Package
import net.adoptopenjdk.api.v3.models.Project
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import org.junit.jupiter.api.Test

class AdoptReposTest : UpdaterTest() {

    @Test
    fun repoEqualityCheckIsCorrect() {
        runBlocking {
            val time = TimeSource.now()
            val a = formFeatureRelease(time)
            val b = formFeatureRelease(time)

            assertTrue { a == b }
        }
    }

    private fun formFeatureRelease(time: ZonedDateTime): FeatureRelease {
        return FeatureRelease(8, Releases(listOf(Release(
                "a",
                ReleaseType.ga,
                "b",
                "c",
                TimeSource.now(),
                time,
                arrayOf(Binary(
                        Package("a",
                                "b",
                                1L,
                                "v",
                                "c",
                                3,
                                "d"
                        ),
                        2L,
                        time,
                        "d",
                        Installer("a",
                                "b",
                                1L,
                                "v",
                                "c",
                                4),
                        HeapSize.normal,
                        OperatingSystem.linux,
                        Architecture.x64,
                        ImageType.jdk,
                        JvmImpl.hotspot,
                        Project.jdk
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
