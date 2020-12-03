package net.adoptopenjdk.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.APIDataStoreImpl
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class APIDataStoreTest : MongoTest() {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    @Test
    fun reposHasElements() {
        runBlocking {
            val repo = getInitialRepo()
            assert(repo.getFeatureRelease(8)!!.releases.getReleases().toList().size > 0)
        }
    }

    @Test
    fun dataIsStoredToDbCorrectly() {
        runBlocking {
            val repo = getInitialRepo()
            ApiPersistenceFactory.get().updateAllRepos(repo, "")
            val dbData = apiDataStore.loadDataFromDb(false)

            JSONAssert.assertEquals(
                UpdaterJsonMapper.mapper.writeValueAsString(dbData),
                UpdaterJsonMapper.mapper.writeValueAsString(repo),
                true
            )
        }
    }

    @Test
    fun `updated at is set`() {
        runBlocking {
            val repo = getInitialRepo()
            ApiPersistenceFactory.get().updateAllRepos(repo, "")
            val time = TimeSource.now()
            delay(1000)
            ApiPersistenceFactory.get().updateAllRepos(repo, "a-checksum")

            val updatedTime = ApiPersistenceFactory.get().getUpdatedAt()

            assertTrue(updatedTime.time.isAfter(time))
            assertEquals("a-checksum", updatedTime.checksum)
        }
    }

    @Test
    fun `update is not scheduled by default`() {
        assertNull((apiDataStore as APIDataStoreImpl).schedule)
    }
}
