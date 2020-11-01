package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.APIDataStoreImpl
import net.adoptopenjdk.api.v3.dataSources.SortMethod
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.models.*
import net.adoptopenjdk.api.v3.routes.AssetsResource
import org.jboss.weld.junit5.EnableWeld
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime
import kotlin.test.assertEquals

@ExtendWith(value = [DbExtension::class])
@QuarkusTest
@EnableWeld
class AssetsResourceFeatureReleasePathSortOrderTest : FrontendTest() {

    companion object {
        private lateinit var assetResource: AssetsResource
        private lateinit var apiDatastore: APIDataStoreImpl

        @BeforeAll
        @JvmStatic
        fun setup() {
            apiDatastore = APIDataStoreImpl(createRepo())
            assetResource = AssetsResource(apiDatastore)
        }

        private fun createRepo(): AdoptRepos {
            val binary = Binary(
                Package(
                    "a",
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
                Installer(
                    "a",
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

            val repo = AdoptRepos(
                listOf(
                    FeatureRelease(
                        8,
                        Releases(
                            listOf(
                                Release(
                                    "foo", ReleaseType.ga, "a", "foo",
                                    ZonedDateTime.of(2010, 1, 1, 1, 1, 0, 0, TimeSource.ZONE),
                                    ZonedDateTime.of(2010, 1, 1, 1, 1, 0, 0, TimeSource.ZONE),
                                    arrayOf(binary), 2, Vendor.adoptopenjdk,
                                    VersionData(8, 0, 242, "b", null, 4, "b", "8u242-b04_openj9-0.18.0-m1")
                                ),

                                Release(
                                    "bar", ReleaseType.ga, "a", "bar",
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

    @Test
    fun doesSortObaySortMethod() {
        runBlocking {
            assertEquals("bar", getRelease(assetResource, SortOrder.DESC, SortMethod.DATE)[0].id)
            assertEquals("foo", getRelease(assetResource, SortOrder.DESC, SortMethod.DEFAULT)[0].id)
            assertEquals("foo", getRelease(assetResource, SortOrder.DESC, null)[0].id)
        }
    }

    private fun getRelease(assetResource: AssetsResource, sortOrder: SortOrder, sortMethod: SortMethod?): List<Release> {
        return assetResource.get(
            version = 8, release_type = ReleaseType.ga, sortOrder = sortOrder, sortMethod = sortMethod,
            arch = null, heap_size = null, jvm_impl = null, image_type = null, os = null, page = null, pageSize = null, project = null, vendor = null, before = null
        )
    }

    @Test
    fun doesSortOrderIgnoreOpt() {
        runBlocking {
            val releases = getRelease(assetResource, SortOrder.DESC, null)
            assertEquals("bar", releases.get(1).id)
        }
    }
}
