package net.adoptopenjdk.api.v3.dataSources

import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.UpdatedInfo
import net.adoptopenjdk.api.v3.models.Platforms
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.Variants
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

object APIDataStore {
    private var updatedAt: UpdatedInfo = UpdatedInfo(ZonedDateTime.now().minusYears(10), "111")
    private var binaryRepos: AdoptRepos
    private var releaseInfo: ReleaseInfo
    var schedule: ScheduledFuture<*>? = null

    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    val platforms: Platforms
    val variants: Variants

    init {
        val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
        platforms = JsonMapper.mapper.readValue(platformData, Platforms::class.java)

        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = JsonMapper.mapper.readValue(variantData, Variants::class.java)

        binaryRepos = try {
            loadDataFromDb(true)
        } catch (e: Exception) {
            LOGGER.error("Failed to read db", e)
            AdoptRepos(listOf())
        }

        releaseInfo = loadReleaseInfo()
    }

    fun schedulePeriodicUpdates() {
        if (schedule == null) {
            schedule = Executors
                .newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(
                    timerTask {
                        periodicUpdate()
                    },
                    0, 1, TimeUnit.MINUTES
                )
        }
    }

    fun loadReleaseInfo(): ReleaseInfo {
        releaseInfo = runBlocking {
            val releaseInfo = try {
                ApiPersistenceFactory.get().getReleaseInfo()
            } catch (e: Exception) {
                LOGGER.error("Failed to read db", e)
                null
            }

            // Default for first time when DB is still being populated
            releaseInfo ?: ReleaseInfo(
                arrayOf(8, 9, 10, 11, 12, 13, 14),
                arrayOf(9, 10, 12, 13, 14, 15),
                arrayOf(8, 11),
                11,
                14,
                15,
                15
            )
        }
        return releaseInfo
    }

    @VisibleForTesting
    fun loadDataFromDb(forceUpdate: Boolean): AdoptRepos {
        val previousRepo: AdoptRepos? = binaryRepos

        binaryRepos = runBlocking {
            val updated = ApiPersistenceFactory.get().getUpdatedAt()

            if (forceUpdate || updated != updatedAt) {
                val data = variants
                    .versions
                    .map { version ->
                        ApiPersistenceFactory.get().readReleaseData(version)
                    }
                    .filter { it.releases.nodes.isNotEmpty() }
                    .toList()
                updatedAt = ApiPersistenceFactory.get().getUpdatedAt()

                LOGGER.info("Loaded Version: $updatedAt")

                val newData = AdoptRepos(data)
                showStats(previousRepo, newData)
                newData
            } else {
                binaryRepos
            }
        }

        return binaryRepos
    }

    fun getAdoptRepos(): AdoptRepos {
        return binaryRepos
    }

    fun setAdoptRepos(binaryRepos: AdoptRepos) {
        this.binaryRepos = binaryRepos
    }

    private fun periodicUpdate() {
        // Must catch errors or may kill the scheduler
        try {
            binaryRepos = loadDataFromDb(false)
            releaseInfo = loadReleaseInfo()
        } catch (e: Exception) {
            LOGGER.error("Failed to load db", e)
        }
    }

    private fun showStats(binaryRepos: AdoptRepos?, newData: AdoptRepos) {
        newData.allReleases.getReleases()
            .forEach { release ->
                val oldRelease = binaryRepos?.allReleases?.nodes?.get(release.id)
                if (oldRelease == null) {
                    LOGGER.info("New release: ${release.release_name} ${release.binaries.size}")
                } else if (oldRelease.binaries.size != release.binaries.size) {
                    LOGGER.info("Binary count changed ${release.release_name} ${oldRelease.binaries.size} -> ${release.binaries.size}")
                }
            }

        binaryRepos?.allReleases?.getReleases()
            ?.forEach { oldRelease ->
                val newRelease = binaryRepos.allReleases.nodes[oldRelease.id]
                if (newRelease == null) {
                    LOGGER.info("Removed release: ${oldRelease.release_name} ${oldRelease.binaries.size}")
                }
            }
    }

    fun getReleaseInfo(): ReleaseInfo {
        return releaseInfo
    }
}
