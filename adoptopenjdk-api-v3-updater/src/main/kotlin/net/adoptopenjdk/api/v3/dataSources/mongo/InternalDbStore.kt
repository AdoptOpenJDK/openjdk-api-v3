package net.adoptopenjdk.api.v3.dataSources.mongo

import com.mongodb.client.model.UpdateOptions
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

    suspend fun putCachedWebpage(url: String, data: String) {
        webCache.updateOne(
                Document("url", url),
                CacheDbEntry(url, data),
                UpdateOptions().upsert(true),
                false)
    }

    suspend fun getCachedWebpage(url: String): String? {
        return webCache.findOne(Document("url", url))?.data
    }
}