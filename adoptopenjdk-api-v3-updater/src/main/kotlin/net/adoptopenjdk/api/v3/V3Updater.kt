package net.adoptopenjdk.api.v3

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.ai.AppInsightsTelemetry
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ReleaseVersionResolver
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.Variants
import net.adoptopenjdk.api.v3.stats.StatsInterface
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.concurrent.timerTask

class V3Updater {
    private var releaseVersionResolver: ReleaseVersionResolver
    private var apiDataStore: APIDataStore
    private var adoptReposBuilder: AdoptReposBuilder
    private val database: ApiPersistence
    private val variants: Variants
    private var repo: AdoptRepos
    private val statsInterface: StatsInterface

    @Inject
    constructor(
        adoptReposBuilder: AdoptReposBuilder,
        apiDataStore: APIDataStore,
        database: ApiPersistence,
        releaseVersionResolver: ReleaseVersionResolver,
        statsInterface: StatsInterface
    ) {
        this.adoptReposBuilder = adoptReposBuilder
        this.apiDataStore = apiDataStore
        this.releaseVersionResolver = releaseVersionResolver

        AppInsightsTelemetry.start()

        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = UpdaterJsonMapper.mapper.readValue(variantData, Variants::class.java)
        this.database = database

        repo = try {
            apiDataStore.loadDataFromDb(true)
        } catch (e: java.lang.Exception) {
            LOGGER.error("Failed to load db", e)
            AdoptRepos(emptyList())
        }
        this.statsInterface = statsInterface
    }

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        fun calculateChecksum(repo: AdoptRepos): String {
            val md = MessageDigest.getInstance("MD5")
            val outputStream = object : OutputStream() {
                override fun write(b: Int) {
                    md.update(b.toByte())
                }
            }
            UpdaterJsonMapper.mapper.writeValue(outputStream, repo)

            return String(Base64.getEncoder().encode(md.digest()))
        }
    }

    fun incrementalUpdate(repo: AdoptRepos, database: ApiPersistence): AdoptRepos {
        return runBlocking {
            // Must catch errors or may kill the scheduler
            try {
                LOGGER.info("Starting Incremental update")
                val updatedRepo = adoptReposBuilder.incrementalUpdate(repo)

                if (updatedRepo != repo) {
                    val checksum = calculateChecksum(updatedRepo)

                    database.updateAllRepos(repo, checksum)
                    releaseVersionResolver.updateDbVersion(repo)
                    LOGGER.info("Incremental update done")
                    LOGGER.info("Saved version: $checksum")
                }
                return@runBlocking updatedRepo
            } catch (e: Exception) {
                LOGGER.error("Failed to perform incremental update", e)
            } catch (e: Throwable) {
                // Log and rethrow
                LOGGER.error("Error during incremental update", e)
                throw e
            }
            repo
        }
    }

    fun run(instantFullUpdate: Boolean) {
        val executor = Executors.newSingleThreadScheduledExecutor()
        if (instantFullUpdate) {
            executor.scheduleWithFixedDelay(
                timerTask {
                    fullUpdate()

                    executor.scheduleWithFixedDelay(
                        timerTask {
                            repo = incrementalUpdate(repo, database)
                        },
                        1, 3, TimeUnit.MINUTES
                    )
                },
                0, 1, TimeUnit.DAYS
            )
        } else {
            executor.scheduleWithFixedDelay(
                timerTask {
                    fullUpdate()
                },
                1, 1, TimeUnit.DAYS
            )

            executor.scheduleWithFixedDelay(
                timerTask {
                    repo = incrementalUpdate(repo, database)
                },
                1, 3, TimeUnit.MINUTES
            )
        }
    }

    private fun fullUpdate() {
        // Must catch errors or may kill the scheduler
        try {
            runBlocking {
                LOGGER.info("Starting Full update")
                repo = adoptReposBuilder.build(variants.versions)

                val checksum = calculateChecksum(repo)

                database.updateAllRepos(repo, checksum)
                statsInterface.update(repo)
                releaseVersionResolver.updateDbVersion(repo)
                LOGGER.info("Full update done")
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to perform full update", e)
        } catch (e: Throwable) {
            // Log and rethrow, may be unrecoverable error such as OutOfMemoryError
            LOGGER.error("Error during full update", e)
            throw e
        }
    }
}
