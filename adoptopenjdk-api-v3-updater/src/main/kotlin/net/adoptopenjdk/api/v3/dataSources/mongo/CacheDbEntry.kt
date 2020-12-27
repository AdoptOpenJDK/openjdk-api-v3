package net.adoptopenjdk.api.v3.dataSources.mongo

import java.time.ZonedDateTime

class CacheDbEntry(
    val url: String,
    val lastModified: String? = null,
    val lastChecked: ZonedDateTime? = null,
    val data: String?
)
