package net.adoptopenjdk.api.v3

import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHReleaseSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import net.adoptopenjdk.api.v3.models.Release
import org.slf4j.LoggerFactory

object AdoptReposBuilder {

    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    private val excluded: MutableSet<String> = HashSet()

    suspend fun incrementalUpdate(repo: AdoptRepos): AdoptRepos {
        val updated = repo
                .repos
                .map { entry -> getUpdatedFeatureRelease(entry, repo) }
                .filterNotNull()

        return AdoptRepos(updated)
    }

    private suspend fun getUpdatedFeatureRelease(entry: Map.Entry<Int, FeatureRelease>, repo: AdoptRepos): FeatureRelease? {
        val summary = AdoptRepositoryFactory.getAdoptRepository().getSummary(entry.key)

        // Update cycle
        // 1) remove missing ones
        // 2) add new ones
        // 3) fix updated
        val existingRelease = repo.getFeatureRelease(entry.key)
        return if (existingRelease != null) {
            val ids = summary.releases.getIds()

            // keep only release ids that still exist
            val pruned = existingRelease.retain(ids)

            // Find newly added releases
            val newReleases = getNewReleases(summary, pruned)
            val updatedReleases = getUpdatedReleases(summary, pruned)

            pruned
                    .add(newReleases)
                    .add(updatedReleases)
        } else {
            val newReleases = getNewReleases(summary, FeatureRelease(entry.key, emptyList()))
            FeatureRelease(entry.key, Releases(newReleases))
        }
    }

    private suspend fun getUpdatedReleases(summary: GHRepositorySummary, pruned: FeatureRelease): List<Release> {
        return summary.releases.releases
                .filter { !excluded.contains(it.id) }
                .filter { !pruned.releases.hasReleaseBeenUpdated(it.id, it.getUpdatedTime()) }
                .filter { isReleaseOldEnough(it.publishedAt) } // Ignore artifacts for the first 10 min while they are still uploading
                .mapNotNull { getReleaseById(it) }
    }

    private suspend fun getNewReleases(summary: GHRepositorySummary, currentRelease: FeatureRelease): List<Release> {
        return summary.releases.releases
                .filter { !excluded.contains(it.id) }
                .filter { !currentRelease.releases.hasReleaseId(it.id) }
                .filter { isReleaseOldEnough(it.publishedAt) } // Ignore artifacts for the first 10 min while they are still uploading
                .mapNotNull { getReleaseById(it) }
    }

    private fun isReleaseOldEnough(timestamp: String): Boolean {
        val created = ReleaseMapper.parseDate(timestamp)
        return ChronoUnit.MINUTES.between(created, TimeSource.now()).absoluteValue > 10
    }

    private suspend fun getReleaseById(it: GHReleaseSummary): Release? {
        return try {
            return AdoptRepositoryFactory.getAdoptRepository().getReleaseById(it.id)
        } catch (e: Exception) {
            LOGGER.info("Excluding ${it.id} from update")
            excluded.add(it.id)
            null
        }
    }

    suspend fun build(versions: Array<Int>): AdoptRepos {
        excluded.clear()
        // Fetch repos in parallel
        val reposMap = versions
                .reversed()
                .map { version ->
                    AdoptRepositoryFactory.getAdoptRepository().getRelease(version)
                }
                .filterNotNull()
                .map { Pair(it.featureVersion, it) }
                .toMap()
        LOGGER.info("DONE")
        return AdoptRepos(reposMap)
    }
}
