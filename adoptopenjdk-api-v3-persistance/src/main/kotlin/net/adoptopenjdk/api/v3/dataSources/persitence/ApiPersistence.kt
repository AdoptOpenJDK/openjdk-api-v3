package net.adoptopenjdk.api.v3.dataSources.persitence

import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import java.time.ZonedDateTime

interface ApiPersistence {
    suspend fun updateAllRepos(repos: AdoptRepos)
    suspend fun readReleaseData(featureVersion: Int): FeatureRelease

    suspend fun addGithubDownloadStatsEntries(stats: List<GithubDownloadStatsDbEntry>)
    suspend fun getStatsForFeatureVersion(featureVersion: Int): List<GithubDownloadStatsDbEntry>
    suspend fun getLatestGithubStatsForFeatureVersion(featureVersion: Int): GithubDownloadStatsDbEntry?
    suspend fun getGithubStats(start: ZonedDateTime, end: ZonedDateTime): List<GithubDownloadStatsDbEntry>
    suspend fun getDockerStats(start: ZonedDateTime, end: ZonedDateTime): List<DockerDownloadStatsDbEntry>
    suspend fun addDockerDownloadStatsEntries(stats: List<DockerDownloadStatsDbEntry>)
    suspend fun getLatestAllDockerStats(): List<DockerDownloadStatsDbEntry>
    suspend fun removeStatsBetween(start: ZonedDateTime, end: ZonedDateTime)
}
