package net.adoptopenjdk.api.v3.models

import java.time.ZonedDateTime

class DockerDownloadStatsDbEntry(
    date: ZonedDateTime,
    val pulls: Long,
    val repo: String
) : DbStatsEntry<String>(date) {
    override fun getMetric(): Long = pulls
    override fun getId(): String = repo
}
