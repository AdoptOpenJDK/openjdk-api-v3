package net.adoptopenjdk.api.v3.dataSources.persitence.mongo

import javax.inject.Inject
import javax.inject.Singleton
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.reactivestreams.client.ClientSession
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.Release
import org.bson.BsonArray
import org.bson.BsonDateTime
import org.bson.BsonDocument
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineCollection
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

@Singleton
class MongoApiPersistence @Inject constructor(mongoClient: MongoClient) : MongoInterface(mongoClient), ApiPersistence {
    private val releasesCollection: CoroutineCollection<Release> = createCollection(database, RELEASE_DB)
    private val githubStatsCollection: CoroutineCollection<GithubDownloadStatsDbEntry> = createCollection(database, GITHUB_STATS_DB)
    private val dockerStatsCollection: CoroutineCollection<DockerDownloadStatsDbEntry> = createCollection(database, DOCKER_STATS_DB)

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
        const val RELEASE_DB = "release"
        const val GITHUB_STATS_DB = "githubStats"
        const val DOCKER_STATS_DB = "dockerStats"
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

    private suspend fun writeReleases(session: ClientSession?, featureVersion: Int, value: FeatureRelease) {
        val toAdd = value.releases.getReleases().toList()
        if (toAdd.isNotEmpty()) {
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

    override suspend fun addGithubDownloadStatsEntries(stats: List<GithubDownloadStatsDbEntry>) {
        githubStatsCollection.insertMany(stats)
    }

    override suspend fun getStatsForFeatureVersion(featureVersion: Int): List<GithubDownloadStatsDbEntry> {
        return githubStatsCollection.find(Document("version.major", featureVersion))
            .toList()
    }

    override suspend fun getLatestGithubStatsForFeatureVersion(featureVersion: Int): GithubDownloadStatsDbEntry? {
        return githubStatsCollection
            .find(Document("feature_version", featureVersion))
            .sort(Document("date", -1))
            .limit(1)
            .first()
    }

    override suspend fun getGithubStats(start: ZonedDateTime, end: ZonedDateTime): List<GithubDownloadStatsDbEntry> {
        return githubStatsCollection
            .find(betweenDates(start, end))
            .sort(Document("date", 1))
            .toList()
    }

    override suspend fun getDockerStats(start: ZonedDateTime, end: ZonedDateTime): List<DockerDownloadStatsDbEntry> {
        return dockerStatsCollection
            .find(betweenDates(start, end))
            .sort(Document("date", 1))
            .toList()
    }

    override suspend fun addDockerDownloadStatsEntries(stats: List<DockerDownloadStatsDbEntry>) {
        dockerStatsCollection.insertMany(stats)
    }

    override suspend fun getLatestAllDockerStats(): List<DockerDownloadStatsDbEntry> {

        val repoNames = dockerStatsCollection.distinct<String>("repo").toList()

        return repoNames
            .mapNotNull {
                dockerStatsCollection
                    .find(Document("repo", it))
                    .sort(Document("date", -1))
                    .limit(1)
                    .first()
            }
            .toList()
    }

    override suspend fun removeStatsBetween(start: ZonedDateTime, end: ZonedDateTime) {
        val deleteQuery = betweenDates(start, end)
        dockerStatsCollection.deleteMany(deleteQuery)
        githubStatsCollection.deleteMany(deleteQuery)
    }

    private fun betweenDates(start: ZonedDateTime, end: ZonedDateTime): Document {
        return Document("\$and",
            BsonArray(listOf(
                BsonDocument("date", BsonDocument("\$gt", BsonDateTime(start.toInstant().toEpochMilli()))),
                BsonDocument("date", BsonDocument("\$lt", BsonDateTime(end.toInstant().toEpochMilli())))
            )
            )
        )
    }

    private fun majorVersionMatcher(featureVersion: Int) = Document("version_data.major", featureVersion)
}
