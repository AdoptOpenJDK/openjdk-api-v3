package net.adoptopenjdk.api.v3.dataSources

import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.dataSources.persitence.ReleaseInfoFactory
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.Variants
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.timerTask

@Singleton
class APIDataStore @Inject constructor(
    private val apiPersistence: ApiPersistence,
    private val variants: Variants,
    private val releaseInfoFactory: ReleaseInfoFactory
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    private var binaryRepos: AdoptRepos
    var releaseInfo: ReleaseInfo

    init {
        binaryRepos = try {
            loadDataFromDb()
        } catch (e: Exception) {
            LOGGER.error("Failed to read db", e)
            AdoptRepos(listOf())
        }
        releaseInfo = releaseInfoFactory.formReleaseInfo(binaryRepos, variants)
        val disableUpdate = System.getProperty("DISABLE_UPDATE", "false")!!.toBoolean()
        if (!disableUpdate) {
            Executors
                .newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(timerTask {
                    periodicUpdate()
                }, 0, 15, TimeUnit.MINUTES)
        }
    }

    @VisibleForTesting
    fun loadDataFromDb(): AdoptRepos {
        binaryRepos = runBlocking {
            val data = variants
                .versions
                .map { version ->
                    apiPersistence.readReleaseData(version)
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
        } catch (e: Exception) {
            LOGGER.error("Failed to load db", e)
        }
        releaseInfo = releaseInfoFactory.formReleaseInfo(binaryRepos, variants)
    }
}
