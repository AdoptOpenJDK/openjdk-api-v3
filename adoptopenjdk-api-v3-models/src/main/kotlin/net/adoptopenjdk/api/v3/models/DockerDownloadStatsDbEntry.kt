package net.adoptopenjdk.api.v3.models;

import java.time.LocalDateTime

class DockerDownloadStatsDbEntry(
        date: LocalDateTime,
        val pulls: Long,
        val repo: String
) : DbStatsEntry<String>(date) {
    override fun getMetric(): Long = pulls
    override fun getId(): String = repo
}
