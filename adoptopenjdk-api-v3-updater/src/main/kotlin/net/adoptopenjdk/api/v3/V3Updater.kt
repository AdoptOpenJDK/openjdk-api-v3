package net.adoptopenjdk.api.v3

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.ReleaseVersionResolver
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.Variants
import net.adoptopenjdk.api.v3.stats.StatsInterface
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class V3Updater {
    private var database: ApiPersistence
    private val variants: Variants
    private var repo: AdoptRepos
    private val statsInterface: StatsInterface

    init {
        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = UpdaterJsonMapper.mapper.readValue(variantData, Variants::class.java)
        database = ApiPersistenceFactory.get()
        repo = try {
            APIDataStore.loadDataFromDb(true)
        } catch (e: java.lang.Exception) {
            LOGGER.error("Failed to load db", e)
            AdoptRepos(emptyList())
        }
        statsInterface = StatsInterface()
    }

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            V3Updater().run(true)
        }

        fun incrementalUpdate(repo: AdoptRepos, database: ApiPersistence): AdoptRepos {
            return runBlocking {
                // Must catch errors or may kill the scheduler
                try {
                    LOGGER.info("Starting Incremental update")
                    val updatedRepo = AdoptReposBuilder.incrementalUpdate(repo)

                    if (updatedRepo != repo) {
                        val checksum = calculateChecksum(updatedRepo)

                        database.updateAllRepos(repo, checksum)
                        ReleaseVersionResolver.updateDbVersion(repo)
                        LOGGER.info("Incremental update done")
                        LOGGER.info("Saved version: $checksum")
                    }
                    return@runBlocking updatedRepo
                } catch (e: Exception) {
                    LOGGER.error("Failed to perform incremental update", e)
                }
                repo
            }
        }

        fun calculateChecksum(repo: AdoptRepos): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(UpdaterJsonMapper.mapper.writeValueAsString(repo).toByteArray())
            return String(Base64.getEncoder().encode(digest))
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
            repo = incrementalUpdate(repo, database)
        }, incrementalUpdateDelay.toLong(), 3, TimeUnit.MINUTES)
    }

    private fun fullUpdate() {
        // Must catch errors or may kill the scheduler
        try {
            runBlocking {
                LOGGER.info("Starting Full update")
                repo = AdoptReposBuilder.build(variants.versions)

                val checksum = calculateChecksum(repo)

                database.updateAllRepos(repo, checksum)
                statsInterface.update(repo)
                ReleaseVersionResolver.updateDbVersion(repo)
                LOGGER.info("Full update done")
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to perform full update", e)
        }
    }
}
