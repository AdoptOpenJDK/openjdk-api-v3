package net.adoptopenjdk.api.v3.dataSources.persitence

import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.models.DownloadStatsDbEntry

interface ApiPersistence {
    suspend fun updateAllRepos(repos: AdoptRepos)
    suspend fun readReleaseData(featureVersion: Int): FeatureRelease

    suspend fun addDownloadStatsEntries(stats: List<DownloadStatsDbEntry>)
    suspend fun getStatsForFeatureVersion(featureVersion: Int): List<DownloadStatsDbEntry>
}
