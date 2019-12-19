package net.adoptopenjdk.api.v3.dataSources.persitence.mongo

import com.mongodb.client.model.InsertManyOptions
import com.mongodb.reactivestreams.client.ClientSession
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.DownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.Release
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.LoggerFactory


class MongoApiPersistence : ApiPersistence {

    val releasesCollection: CoroutineCollection<Release>
    val githubStatsCollection: CoroutineCollection<DownloadStatsDbEntry>
    val dockerStatsCollection: CoroutineCollection<DockerDownloadStatsDbEntry>
    val client: CoroutineClient

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    init {
        val dbName = System.getenv("MONGODB_DBNAME") ?: "api-data"
        val username = System.getenv("MONGODB_USER")
        val password = System.getenv("MONGODB_PASSWORD")
        val host = System.getenv("MONGODB_HOST") ?: "localhost"
        val port = System.getenv("MONGODB_PORT") ?: "27017"

        LOGGER.info("Connecting to mongodb://${username}:a-password@${host}:${port}/${dbName}")
        var uri: String
        if (username != null && password != null) {
            uri = "mongodb://${username}:${password}@${host}:${port}/${dbName}"
        } else {
            uri = "mongodb://${host}:${port}"
        }

        if (System.getProperty("MONGO_DB") != null) {
            uri = System.getProperty("MONGO_DB")
        }

        client = KMongo.createClient(uri).coroutine
        val database = client.getDatabase(dbName)

        runBlocking {
            if (!database.listCollectionNames().contains("release")) {
                //TODO add indexes
                database.createCollection("release")
            }

            if (!database.listCollectionNames().contains("githubStats")) {
                //TODO add indexes
                database.createCollection("githubStats")
            }

            if (!database.listCollectionNames().contains("dockerStats")) {
                //TODO add indexes
                database.createCollection("dockerStats")
            }
        }
        releasesCollection = database.getCollection("release")
        githubStatsCollection = database.getCollection("githubStats")
        dockerStatsCollection = database.getCollection("dockerStats")
    }

    override suspend fun updateAllRepos(repos: AdoptRepos) {

        var session: ClientSession? = null

        try {
            session = client.startSession()
        } catch (e: Exception) {
            LOGGER.warn("DB does not support transactions")
        }
        try {
            session?.startTransaction()
            repos
                    .repos
                    .forEach { repo ->
                        writeReleases(session, repo.key, repo.value)
                    }

        } finally {
            session?.commitTransaction()
            session?.close()
        }
    }

    suspend fun writeReleases(session: ClientSession?, featureVersion: Int, value: FeatureRelease) {
        val toAdd = value.releases.getReleases().toList()
        if (toAdd.size > 0) {
            if (session == null) {
                releasesCollection.deleteMany(majorVersionMatcher(featureVersion))
                releasesCollection.insertMany(toAdd, InsertManyOptions())
            } else {
                releasesCollection.deleteMany(session, majorVersionMatcher(featureVersion))
                releasesCollection.insertMany(session, toAdd, InsertManyOptions())
            }
        }
    }

    override suspend fun readReleaseData(featureVersion: Int): FeatureRelease {
        val releases = releasesCollection
                .find(majorVersionMatcher(featureVersion))
                .toList()

        return FeatureRelease(featureVersion, Releases(releases))
    }

    override suspend fun addDownloadStatsEntries(stats: List<DownloadStatsDbEntry>) {
        githubStatsCollection.insertMany(stats)
    }

    override suspend fun getStatsForFeatureVersion(featureVersion: Int): List<DownloadStatsDbEntry> {
        return githubStatsCollection.find(Document("version.major", featureVersion))
                .toList()
    }

    private fun majorVersionMatcher(featureVersion: Int) = Document("version_data.major", featureVersion)
}