package net.adoptopenjdk.api.v3.dataSources.persitence.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
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

class MongoClient {
    val database: CoroutineDatabase
    val client: CoroutineClient

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
        private const val DEFAULT_DBNAME = "api-data"
        private const val DEFAULT_HOST = "localhost"
        private const val DEFAULT_PORT = "27017"
        private const val DEFAULT_SERVER_SELECTION_TIMEOUT_MILLIS = "100"

        fun createConnectionString(
            dbName: String,
            username: String? = null,
            password: String? = null,
            host: String? = DEFAULT_HOST,
            port: String? = DEFAULT_PORT,
            serverSelectionTimeoutMills: String? = DEFAULT_SERVER_SELECTION_TIMEOUT_MILLIS
        ): String {
            return System.getProperty("MONGODB_TEST_CONNECTION_STRING")
                ?: if (username != null && password != null) {
                    LOGGER.info("Connecting to mongodb://$username:a-password@$host:$port/$dbName")
                    "mongodb://$username:$password@$host:$port/$dbName"
                } else {
                    val developmentConnectionString = "mongodb://$host:$port/?serverSelectionTimeoutMS=$serverSelectionTimeoutMills"
                    LOGGER.info("Using development connection string - $developmentConnectionString")
                    developmentConnectionString
                }
        }
    }

    init {
        val dbName = System.getenv("MONGODB_DBNAME") ?: DEFAULT_DBNAME
        val connectionString = createConnectionString(
            dbName,
            username = System.getenv("MONGODB_USER"),
            password = System.getenv("MONGODB_PASSWORD"),
            host = System.getenv("MONGODB_HOST"),
            port = System.getenv("MONGODB_PORT"),
            serverSelectionTimeoutMills = System.getenv("MONGODB_SERVER_SELECTION_TIMEOUT_MILLIS")
        )

        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(connectionString))
            .build()

        client = KMongo.createClient(settings).coroutine
        database = client.getDatabase(dbName)
    }
}
