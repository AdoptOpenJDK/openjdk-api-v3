package net.adoptopenjdk.api.v3.dataSources

import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.models.Platforms
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.Variants
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

object APIDataStore {
    private var binaryRepos: AdoptRepos
    private var releaseInfo: ReleaseInfo

    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    val platforms: Platforms
    val variants: Variants

    init {

        val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
        platforms = JsonMapper.mapper.readValue(platformData, Platforms::class.java)

        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = JsonMapper.mapper.readValue(variantData, Variants::class.java)

        try {
            binaryRepos = loadDataFromDb()
        } catch (e: Exception) {
            LOGGER.error("Failed to read db", e)
            binaryRepos = AdoptRepos(listOf())
        }

        releaseInfo = loadReleaseInfo()

        Executors
            .newSingleThreadScheduledExecutor()
            .scheduleWithFixedDelay(timerTask {
                periodicUpdate()
            }, 0, 15, TimeUnit.MINUTES)
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
    fun loadDataFromDb(): AdoptRepos {
        binaryRepos = runBlocking {
            val data = variants
                .versions
                .map { version ->
                    ApiPersistenceFactory.get().readReleaseData(version)
                }
                .filter { it.releases.nodes.isNotEmpty() }
                .toList()

            AdoptRepos(data)
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
            binaryRepos = loadDataFromDb()
            releaseInfo = loadReleaseInfo()
        } catch (e: Exception) {
            LOGGER.error("Failed to load db", e)
        }
    }

    fun getReleaseInfo(): ReleaseInfo {
        return releaseInfo
    }
}
