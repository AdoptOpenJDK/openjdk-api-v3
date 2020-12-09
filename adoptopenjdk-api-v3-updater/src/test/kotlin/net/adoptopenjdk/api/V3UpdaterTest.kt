package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.V3Updater
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import org.jboss.weld.junit5.auto.AddPackages
import org.jboss.weld.junit5.auto.EnableAutoWeld
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnableAutoWeld
@AddPackages(value = [AdoptReposBuilder::class, APIDataStore::class])
class V3UpdaterTest : MongoTest() {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    @Test
    fun `updated at check sum is set`(
        adoptReposBuilder: AdoptReposBuilder,
        apiDataStore: APIDataStore
    ) {
        runBlocking {
            val repo = BaseTest.adoptRepos
            ApiPersistenceFactory.get().updateAllRepos(repo, "a-checksum")

            val without8 = AdoptRepos(repo.repos.filterNot { it.key == 8 })
            ApiPersistenceFactory.get().updateAllRepos(without8, "a-different-checksum")

            V3Updater(adoptReposBuilder, apiDataStore).incrementalUpdate(repo, ApiPersistenceFactory.get())

            val updatedTime = ApiPersistenceFactory.get().getUpdatedAt()

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
