package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.V3Updater
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ReleaseVersionResolver
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.stats.StatsInterface
import org.jboss.weld.junit5.auto.AddPackages
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@AddPackages(value = [AdoptReposBuilder::class])
class V3UpdaterTest : MongoTest() {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    @Test
    fun `updated at check sum is set`(
        adoptReposBuilder: AdoptReposBuilder,
        apiDataStore: APIDataStore,
        apiPersistence: ApiPersistence,
        database: ApiPersistence,
        releaseVersionResolver: ReleaseVersionResolver,
        statsInterface: StatsInterface
    ) {
        runBlocking {
            val repo = BaseTest.adoptRepos
            apiPersistence.updateAllRepos(repo, "a-checksum")

            val without8 = AdoptRepos(repo.repos.filterNot { it.key == 8 })
            apiPersistence.updateAllRepos(without8, "a-different-checksum")

            V3Updater(adoptReposBuilder, apiDataStore, database, statsInterface, releaseVersionResolver).incrementalUpdate(repo)

            val updatedTime = apiPersistence.getUpdatedAt()

            assertEquals("a-different-checksum", updatedTime.checksum)
        }
    }

    @Test
    fun `checksum works`() {
        runBlocking {
            val checksum = V3Updater.calculateChecksum(BaseTest.adoptRepos)
            assertTrue(checksum.length == 24)
        }
    }
}
