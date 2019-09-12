package net.adoptopenjdk.api.v3

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.Variants
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask
import org.slf4j.LoggerFactory

class V3Updater {
    private var database: ApiPersistence
    val variants: Variants
    var repo: AdoptRepos

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            V3Updater().run(true)
        }
    }

    init {
        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = JsonMapper.mapper.readValue(variantData, Variants::class.java)
        database = ApiPersistenceFactory.get()
        try {
            repo = APIDataStore.loadDataFromDb()
        } catch (e: java.lang.Exception) {
            repo = AdoptRepos(emptyList())
        }
    }

    fun run(instantFullUpdate: Boolean) {


        val executor = Executors.newSingleThreadScheduledExecutor()

        executor.scheduleWithFixedDelay(timerTask {
            //Full update on boot and every 24h
            fullUpdate()
        }, if (instantFullUpdate) 0 else 1, 1, TimeUnit.DAYS)


        var incrementalUpdateDelay = 0
        if (instantFullUpdate) {
            //if doing a full update wait 30 min before starting
            incrementalUpdateDelay = 30
        }
        executor.scheduleWithFixedDelay(timerTask {
            incrementalUpdate()
        }, incrementalUpdateDelay.toLong(), 1, TimeUnit.MINUTES)

    }

    private fun fullUpdate() {
        //Must catch errors or may kill the scheduler
        try {
            runBlocking {
                repo = AdoptReposBuilder.build(variants.versions)
                database.updateAllRepos(repo)
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to perform full update", e)
        }
    }

    private fun incrementalUpdate() {
        //Must catch errors or may kill the scheduler
        try {
            runBlocking {
                val updatedRepo = AdoptReposBuilder.incrementalUpdate(repo)

                if (updatedRepo != repo) {
                    repo = updatedRepo
                    database.updateAllRepos(repo)
                }

            }
        } catch (e: Exception) {
            LOGGER.error("Failed to perform incremental update", e)
        }
    }


}
