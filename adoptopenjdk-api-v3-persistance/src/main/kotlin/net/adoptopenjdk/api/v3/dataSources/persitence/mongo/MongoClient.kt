package net.adoptopenjdk.api.v3.dataSources.persitence.mongo

import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.LoggerFactory

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

object DefaultMongoClientConfig {
    const val DBNAME = "api-data"
    const val HOST = "localhost"
    const val PORT = "27017"
    const val SERVER_SELECTION_TIMEOUT_MILLIS = 100
}

class MongoClient {
    val database: CoroutineDatabase
    val client: CoroutineClient

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    init {
        val dbName = System.getenv("MONGODB_DBNAME") ?: DefaultMongoClientConfig.DBNAME
        val username = System.getenv("MONGODB_USER")
        val password = System.getenv("MONGODB_PASSWORD")
        val host = System.getenv("MONGODB_HOST") ?: DefaultMongoClientConfig.HOST
        val port = System.getenv("MONGODB_PORT") ?: DefaultMongoClientConfig.PORT

        val uri = System.getProperty("MONGODB_TEST_CONNECTION_STRING")
                ?: if (username != null && password != null) {
                    LOGGER.info("Connecting to mongodb://$username:a-password@$host:$port/$dbName")
                    "mongodb://$username:$password@$host:$port/$dbName"
                } else {
                    val serverSelectionTimeoutMills = System.getenv("MONGODB_SERVER_SELECTION_TIMEOUT_MILLIS") ?: DefaultMongoClientConfig.SERVER_SELECTION_TIMEOUT_MILLIS
                    val developmentConnectionString = "mongodb://$host:$port/?serverSelectionTimeoutMS=$serverSelectionTimeoutMills"
                    LOGGER.info("Using development connection string - $developmentConnectionString")
                    developmentConnectionString
                }

        client = KMongo.createClient(uri).coroutine
        database = client.getDatabase(dbName)
    }
}
