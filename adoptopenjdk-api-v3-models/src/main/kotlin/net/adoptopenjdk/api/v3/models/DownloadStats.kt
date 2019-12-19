package net.adoptopenjdk.api.v3.models

import java.time.LocalDateTime

class DownloadStats(
        val date: LocalDateTime,
        val total_downloads: Long,
        val downloads: Map<Int, Long>,
        val total_daily_downloads: Long,
        val daily_downloads: Map<Int, Long>
)