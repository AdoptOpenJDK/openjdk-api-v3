package net.adoptopenjdk.api.v3.dataSources.mongo

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoClientFactory
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoInterface
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineCollection
import javax.enterprise.inject.Default
import javax.inject.Singleton

interface InternalDbStore {
    suspend fun putCachedWebpage(url: String, lastModified: String?, data: String?)
    suspend fun getCachedWebpage(url: String): CacheDbEntry?
}

@Singleton
@Default
class InternalDbStoreImpl : MongoInterface(MongoClientFactory.get()), InternalDbStore {
    private val webCache: CoroutineCollection<CacheDbEntry> = createCollection(database, "web-cache")

    init {
        runBlocking {
            webCache.createIndex("""{"url":1}""", IndexOptions().background(true))
        }
    }

    override suspend fun putCachedWebpage(url: String, lastModified: String?, data: String?) {
        GlobalScope.launch {
            webCache.updateOne(
                Document("url", url),
                CacheDbEntry(url, lastModified, data),
                UpdateOptions().upsert(true),
                false
            )
        }
    }

    override suspend fun getCachedWebpage(url: String): CacheDbEntry? {
        return webCache.findOne(Document("url", url))
    }
}
