package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue


class APIDataStoreTest : BaseTest() {


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
            ApiPersistenceFactory.get().updateAllRepos(repo)
            val dbData = APIDataStore.loadDataFromDb()
            val before = JsonMapper.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(repo.allReleases.getReleases().toList())
            val after = JsonMapper.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dbData.allReleases.getReleases().toList())

            assertTrue(before.equals(after))
        }
    }

}

