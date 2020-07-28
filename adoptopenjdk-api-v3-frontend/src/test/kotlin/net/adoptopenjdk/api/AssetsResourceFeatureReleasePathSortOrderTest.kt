package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.SortMethod
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
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
import java.time.ZonedDateTime
import kotlin.test.assertEquals

@QuarkusTest
class AssetsResourceFeatureReleasePathSortOrderTest : BaseTest() {

    @Test
    fun doesSortObaySortMethod() {
        runBlocking {
            val repo = createRepo()

            APIDataStore.setAdoptRepos(repo)

            assertEquals("bar", AssetsResourceFeatureReleasePathTest.getReleasesWithSortMethod(SortOrder.DESC, SortMethod.DATE).get(0).id)
            assertEquals("foo", AssetsResourceFeatureReleasePathTest.getReleasesWithSortMethod(SortOrder.DESC, SortMethod.DEFAULT).get(0).id)
            assertEquals("foo", AssetsResourceFeatureReleasePathTest.getReleases(SortOrder.DESC).get(0).id)
        }
    }

    @Test
    fun isDoesSortOrderIgnoreOpt() {
        runBlocking {
            val repo = createRepo()

            APIDataStore.setAdoptRepos(repo)

            val releases = AssetsResourceFeatureReleasePathTest.getReleases(SortOrder.DESC)

            assertEquals("bar", releases.get(1).id)
        }
    }

    private fun createRepo(): AdoptRepos {
        val binary = Binary(
            Package("a",
                "b",
                1L,
                "v",
                "c",
                3,
                "d",
                "e"
            ),
            2L,
            TimeSource.now(),
            "d",
            Installer("a",
                "b",
                1L,
                "v",
                "c",
                4,
                "d,",
                "e"
            ),
            HeapSize.normal,
            OperatingSystem.linux,
            Architecture.x64,
            ImageType.jdk,
            JvmImpl.hotspot,
            Project.jdk
        )

        val repo = AdoptRepos(listOf(
            FeatureRelease(8, Releases(listOf(
                Release("foo", ReleaseType.ga, "a", "foo",
                    ZonedDateTime.of(2010, 1, 1, 1, 1, 0, 0, TimeSource.ZONE),
                    ZonedDateTime.of(2010, 1, 1, 1, 1, 0, 0, TimeSource.ZONE),
                    arrayOf(binary), 2, Vendor.adoptopenjdk,
                    VersionData(8, 0, 242, "b", null, 4, "b", "8u242-b04_openj9-0.18.0-m1")
                ),

                Release("bar", ReleaseType.ga, "a", "bar",
                    ZonedDateTime.of(2010, 1, 2, 1, 1, 0, 0, TimeSource.ZONE),
                    ZonedDateTime.of(2010, 1, 2, 1, 1, 0, 0, TimeSource.ZONE),
                    arrayOf(binary), 2, Vendor.adoptopenjdk,
                    VersionData(8, 0, 242, "a", null, 4, "a", "8u242-b04_openj9-0.18.0-m1")
                )
            )
            )
            )
        )
        )
        return repo
    }
}
