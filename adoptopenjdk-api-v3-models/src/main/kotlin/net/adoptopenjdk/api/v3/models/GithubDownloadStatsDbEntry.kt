package net.adoptopenjdk.api.v3.models

import java.time.LocalDateTime

class GithubDownloadStatsDbEntry(
        date: LocalDateTime,
        val downloads: Long,
        val feature_version: Int
) : DbStatsEntry<Int>(date) {
    override fun getMetric(): Long = downloads
    override fun getId(): Int = feature_version
}