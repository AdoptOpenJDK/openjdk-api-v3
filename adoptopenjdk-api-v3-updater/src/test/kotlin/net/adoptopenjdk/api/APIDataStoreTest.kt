package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.LoggerFactory


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

            JSONAssert.assertEquals(UpdaterJsonMapper.mapper.writeValueAsString(dbData),
                    UpdaterJsonMapper.mapper.writeValueAsString(repo),
                    true)
        }
    }

}

