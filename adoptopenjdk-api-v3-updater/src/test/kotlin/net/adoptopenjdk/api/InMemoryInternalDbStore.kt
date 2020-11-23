package net.adoptopenjdk.api

import net.adoptopenjdk.api.v3.dataSources.mongo.CacheDbEntry
import net.adoptopenjdk.api.v3.dataSources.mongo.InternalDbStore

class InMemoryInternalDbStore : InternalDbStore {
    private val cache: MutableMap<String, CacheDbEntry> = HashMap()
    override suspend fun putCachedWebpage(url: String, lastModified: String?, data: String?) {
        cache[url] = CacheDbEntry(url, lastModified, data)
    }

    override suspend fun getCachedWebpage(url: String): CacheDbEntry? {
        return cache[url]
    }
}
