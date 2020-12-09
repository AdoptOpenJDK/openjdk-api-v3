package net.adoptopenjdk.api.testDoubles

import net.adoptopenjdk.api.v3.dataSources.mongo.CacheDbEntry
import net.adoptopenjdk.api.v3.dataSources.mongo.InternalDbStore
import javax.annotation.Priority
import javax.enterprise.inject.Alternative
import javax.inject.Singleton

@Priority(1)
@Alternative
@Singleton
class InMemoryInternalDbStore : InternalDbStore {
    private val cache: MutableMap<String, CacheDbEntry> = HashMap()
    override suspend fun putCachedWebpage(url: String, lastModified: String?, data: String?) {
        cache[url] = CacheDbEntry(url, lastModified, data)
    }

    override suspend fun getCachedWebpage(url: String): CacheDbEntry? {
        return cache[url]
    }
}
