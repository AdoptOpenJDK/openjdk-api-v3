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


object InternalDbStoreFactory {
    private var impl: InternalDbStore? = null

    fun get(): InternalDbStore {
        if (impl == null) {
            impl = InternalDbStore()
        }
        return impl!!
    }

    fun set(impl: InternalDbStore?) {
        InternalDbStoreFactory.impl = impl
    }
}

class InternalDbStore : MongoInterface(MongoClientFactory.get()) {
    private val webCache: CoroutineCollection<CacheDbEntry> = createCollection(database, "web-cache")

    init {
        runBlocking {
            webCache.createIndex("""{"url":1}""", IndexOptions().background(true))
        }
    }


    suspend fun putCachedWebpage(url: String, lastModified: String?, data: String?) {
        GlobalScope.launch {
            webCache.updateOne(
                    Document("url", url),
                    CacheDbEntry(url, lastModified, data),
                    UpdateOptions().upsert(true),
                    false)
        }
    }

    suspend fun getCachedWebpage(url: String): CacheDbEntry? {
        return webCache.findOne(Document("url", url))
    }
}