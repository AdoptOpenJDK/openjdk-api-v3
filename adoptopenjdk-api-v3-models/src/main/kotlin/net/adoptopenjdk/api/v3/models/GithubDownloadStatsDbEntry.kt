package net.adoptopenjdk.api.v3.models

import java.time.LocalDateTime

class GithubDownloadStatsDbEntry(

        val date: LocalDateTime,
        val downloads: Long,
        val feature_version: Int

)