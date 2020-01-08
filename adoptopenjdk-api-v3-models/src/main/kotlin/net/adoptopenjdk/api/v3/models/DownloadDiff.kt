package net.adoptopenjdk.api.v3.models

import java.time.LocalDateTime

class DownloadDiff(
        val date: LocalDateTime,
        val total: Long,
        val daily: Long
)