package net.adoptopenjdk.api.v3.dataSources.mongo

class CacheDbEntry(
    val url: String,
    val lastModified: String? = null,
    val data: String?
)
