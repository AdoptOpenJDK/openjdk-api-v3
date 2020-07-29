package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.V3Updater
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

class V3UpdaterTest : BaseTest() {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    @Test
    fun `updated at check sum is set`() {
        runBlocking {
            val repo = getInitialRepo()
            ApiPersistenceFactory.get().updateAllRepos(repo, "a-checksum")

            val without8 = AdoptRepos(repo.repos.filterNot { it.key == 8 })
            ApiPersistenceFactory.get().updateAllRepos(without8, "a-different-checksum")

            V3Updater.incrementalUpdate(repo, ApiPersistenceFactory.get())

            val updatedTime = ApiPersistenceFactory.get().getUpdatedAt()

            assertEquals("a-different-checksum", updatedTime.checksum)
        }
    }
}
