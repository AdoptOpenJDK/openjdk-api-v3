package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepo
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.LoggerFactory

class APIDataStoreTest : UpdaterTest() {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    @Test
    fun reposHasElements() {
        runBlocking {
            val repo = getAdoptRepos()
            assert(repo.getFeatureRelease(8)!!.releases.getReleases().toList().size > 0)
        }
    }

    @Test
    fun dataIsStoredToDbCorrectly() {
        runBlocking {
            val apiPersistance = getApiPersistence()
            val dataStore = getApiDataStore()

            val repo = getAdoptRepos()
            val blankRepo = AdoptRepos(repo.repos
                .keys
                .map { f ->
                    f to FeatureRelease(f, listOf(AdoptRepo(listOf(Release("foo" + f, ReleaseType.ga, "a", "b",
                        TimeSource.now().minusMinutes(20),
                        TimeSource.now().minusMinutes(20),
                        arrayOf(), 2, Vendor.adoptopenjdk,
                        VersionData(f, 2, 3, "", 1, 4, "", "")
                    )
                    )
                    )
                    )
                    )
                }
                .toMap())

            apiPersistance.updateAllRepos(blankRepo)
            val blankDbData = dataStore.loadDataFromDb()
            JSONAssert.assertEquals(UpdaterJsonMapper.mapper.writeValueAsString(blankRepo),
                UpdaterJsonMapper.mapper.writeValueAsString(blankDbData),
                true
            )

            apiPersistance.updateAllRepos(repo)
            val dbData = dataStore.loadDataFromDb()
            JSONAssert.assertEquals(UpdaterJsonMapper.mapper.writeValueAsString(repo),
                UpdaterJsonMapper.mapper.writeValueAsString(dbData),
                true
            )
        }
    }
}
