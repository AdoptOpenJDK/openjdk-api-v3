package net.adoptopenjdk.api.v3

import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.RepositorySummary
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.models.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object AdoptReposBuilder {

    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    suspend fun incrementalUpdate(repo: AdoptRepos): AdoptRepos {
        val updated = repo
                .repos
                .map { entry -> getUpdatedFeatureRelease(entry, repo) }
                .filterNotNull()

        return AdoptRepos(updated)
    }

    private suspend fun getUpdatedFeatureRelease(entry: Map.Entry<Int, FeatureRelease>, repo: AdoptRepos): FeatureRelease? {
        val summary = AdoptRepositoryFactory.adoptRepository.getSummary(entry.key)

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

    private suspend fun getUpdatedReleases(summary: RepositorySummary, pruned: FeatureRelease): List<Release> {
        return summary.releases.releases
                .filter { !pruned.releases.hasReleaseBeenUpdated(it.id, it.getUpdatedTime()) }
                .map {
                    AdoptRepositoryFactory.adoptRepository.getReleaseById(it.id)
                }
                .filterNotNull()

    }

    private suspend fun getNewReleases(summary: RepositorySummary, currentRelease: FeatureRelease): List<Release> {
        return summary.releases.releases
                .filter { !currentRelease.releases.hasReleaseId(it.id) }
                .map {
                    AdoptRepositoryFactory.adoptRepository.getReleaseById(it.id)
                }.filterNotNull()

    }


    suspend fun build(versions: List<Int>): AdoptRepos {
        //Fetch repos in parallel
        val reposMap = versions
                .reversed()
                .map { version ->
                    AdoptRepositoryFactory.adoptRepository.getRelease(version)
                }
                .map { Pair(it.featureVersion, it) }
                .toMap()
        LOGGER.info("DONE")
        return AdoptRepos(reposMap)
    }
}