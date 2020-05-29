package net.adoptopenjdk.api.v3.dataSources.persitence.mongo

import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.LoggerFactory
import javax.inject.Singleton

object MongoClientFactory {
    // Current default impl is mongo impl
    private var impl: MongoClient? = null

    fun get(): MongoClient {
        if (impl == null) {
            impl = MongoClient()
        }
        return impl!!
    }

    fun set(impl: MongoClient?) {
        MongoClientFactory.impl = impl
    }
}

@Singleton
class MongoClient {
    private lateinit var dbName: String
    lateinit var database: CoroutineDatabase
    lateinit var client: CoroutineClient

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    constructor() {
        connect(System.getenv("MONGODB_DBNAME") ?: "api-data")
    }

    constructor(dbName: String) {
        connect(dbName)
    }

    fun connect(dbName: String) {
        this.dbName = dbName
        val username = System.getenv("MONGODB_USER")
        val password = System.getenv("MONGODB_PASSWORD")
        val host = System.getenv("MONGODB_HOST") ?: "localhost"
        val port = System.getenv("MONGODB_PORT") ?: "27017"

        LOGGER.info("Connecting to mongodb://$username:a-password@$host:$port/$dbName")
        var uri = if (username != null && password != null) {
            "mongodb://$username:$password@$host:$port/$dbName"
        } else {
            "mongodb://$host:$port"
        }

        if (System.getProperty("MONGO_DB") != null) {
            uri = System.getProperty("MONGO_DB")
        }

        client = KMongo.createClient(uri).coroutine
        database = client.getDatabase(dbName)
    }
}
