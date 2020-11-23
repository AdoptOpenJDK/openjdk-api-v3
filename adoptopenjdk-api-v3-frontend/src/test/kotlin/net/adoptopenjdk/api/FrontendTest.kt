package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import net.adoptopenjdk.api.v3.dataSources.SortMethod
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.filters.BinaryFilter
import net.adoptopenjdk.api.v3.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.models.Binary
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.Vendor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(value = [DbExtension::class])
@QuarkusTest
open class FrontendTest {

    @BeforeEach
    fun initDb() {
        BaseTest.startDb()
        BaseTest.mockRepo()
    }

    protected fun getRandomBinary(): Pair<Release, Binary> {
        val release = BaseTest.adoptRepos.getFilteredReleases(
            ReleaseFilter(featureVersion = 8, vendor = Vendor.adoptopenjdk),
            BinaryFilter(),
            SortOrder.ASC,
            SortMethod.DEFAULT
        ).first()

        val binary = release.binaries.first()
        return Pair(release, binary)
    }
}
