package net.adoptopenjdk.api.v3

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.Variants
import net.adoptopenjdk.api.v3.stats.StatsInterface
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.timerTask

@Singleton
class V3Updater @Inject constructor(
    private val database: ApiPersistence,
    private val apiDataStore: APIDataStore,
    private val statsInterface: StatsInterface,
    private val variants: Variants,
    private val adoptReposBuilder: AdoptReposBuilder
) {
    private var repo: AdoptRepos

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val updater: V3Updater = GuiceBinding.getInjector().getInstance(V3Updater::class.java)

            updater.run(true)
        }
    }

    init {
        repo = try {
            apiDataStore.loadDataFromDb()
        } catch (e: java.lang.Exception) {
            AdoptRepos(emptyList())
        }
    }

    fun run(instantFullUpdate: Boolean) {
        val executor = Executors.newSingleThreadScheduledExecutor()

        executor.scheduleWithFixedDelay(timerTask {
            // Full update on boot and every 24h
            fullUpdate()
        }, if (instantFullUpdate) 0 else 1, 1, TimeUnit.DAYS)

        var incrementalUpdateDelay = 0
        if (instantFullUpdate) {
            // if doing a full update wait 120 min before starting
            incrementalUpdateDelay = 120
        }
        executor.scheduleWithFixedDelay(timerTask {
            incrementalUpdate()
        }, incrementalUpdateDelay.toLong(), 3, TimeUnit.MINUTES)
    }

    private fun fullUpdate() {
        // Must catch errors or may kill the scheduler
        try {
            runBlocking {

                LOGGER.info("Starting Full update")
                repo = adoptReposBuilder.build(variants.versions)
                database.updateAllRepos(repo)
                statsInterface.update(repo)
                LOGGER.info("Full update done")
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to perform full update", e)
        }
    }

    private fun incrementalUpdate() {
        // Must catch errors or may kill the scheduler
        try {
            runBlocking {
                LOGGER.info("Starting Incremental update")
                val updatedRepo = adoptReposBuilder.incrementalUpdate(repo)

                if (updatedRepo != repo) {
                    repo = updatedRepo
                    database.updateAllRepos(repo)
                    LOGGER.info("Incremental update done")
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to perform incremental update", e)
        }
    }
}
