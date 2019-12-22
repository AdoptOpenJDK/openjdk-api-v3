package net.adoptopenjdk.api.v3.models

import java.time.LocalDate

class DownloadDiff(
        val date: LocalDate,
        val total: Long,
        val daily: Long
)