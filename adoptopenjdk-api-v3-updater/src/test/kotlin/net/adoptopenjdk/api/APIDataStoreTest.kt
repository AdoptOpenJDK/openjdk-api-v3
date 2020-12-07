package net.adoptopenjdk.api

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.APIDataStoreImpl
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import org.jboss.weld.junit5.auto.AddPackages
import org.jboss.weld.junit5.auto.EnableAutoWeld
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@EnableAutoWeld
@ExtendWith(MockKExtension::class)
@AddPackages(value = [AdoptReposBuilder::class, APIDataStore::class])
class APIDataStoreTest : MongoTest() {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    @Test
    fun reposHasElements() {
        runBlocking {
            val repo = BaseTest.adoptRepos
            assert(repo.getFeatureRelease(8)!!.releases.getReleases().toList().size > 0)
        }
    }

    @Test
    fun dataIsStoredToDbCorrectly(apiDataStore: APIDataStore) {
        runBlocking {
            ApiPersistenceFactory.get().updateAllRepos(BaseTest.adoptRepos, "")
            val dbData = apiDataStore.loadDataFromDb(false)

            JSONAssert.assertEquals(
                UpdaterJsonMapper.mapper.writeValueAsString(dbData),
                UpdaterJsonMapper.mapper.writeValueAsString(BaseTest.adoptRepos),
                true
            )
        }
    }

    @Test
    fun `updated at is set`() {
        runBlocking {
            ApiPersistenceFactory.get().updateAllRepos(BaseTest.adoptRepos, "")
            val time = TimeSource.now()
            delay(1000)
            ApiPersistenceFactory.get().updateAllRepos(BaseTest.adoptRepos, "a-checksum")

            val updatedTime = ApiPersistenceFactory.get().getUpdatedAt()

            assertTrue(updatedTime.time.isAfter(time))
            assertEquals("a-checksum", updatedTime.checksum)
        }
    }

    @Test
    fun `update is not scheduled by default`(apiDataStore: APIDataStore) {
        assertNull((apiDataStore as APIDataStoreImpl).schedule)
    }
}
