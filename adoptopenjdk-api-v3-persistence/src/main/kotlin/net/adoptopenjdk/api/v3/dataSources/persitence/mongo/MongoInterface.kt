package net.adoptopenjdk.api.v3.dataSources.persitence.mongo

import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase

abstract class MongoInterface(mongoClient: MongoClient) {
    protected val database: CoroutineDatabase = mongoClient.database
    protected val client: CoroutineClient = mongoClient.client

    inline fun <reified T : Any> createCollection(database: CoroutineDatabase, collectionName: String): CoroutineCollection<T> {
        return runBlocking {
            if (!database.listCollectionNames().contains(collectionName)) {
                // TODO add indexes
                database.createCollection(collectionName)
            }
            return@runBlocking database.getCollection<T>(collectionName)
        }
    }
}
