package net.adoptopenjdk.api.v3.models;

import java.time.LocalDateTime

class DockerDownloadStatsDbEntry(
        val date: LocalDateTime,
        val pulls: Long,
        val repo: String
)