package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.*
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.github.graphql.GraphQLGitHubClient
import net.adoptopenjdk.api.v3.models.Release
import org.awaitility.Awaitility
import org.junit.Ignore
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Ignore("For manual execution")
class UpdateRunner : BaseTest() {

    @Test
    @Ignore("For manual execution")
    fun run() {
        System.clearProperty("GITHUB_TOKEN")
        runBlocking {

            var repo = getInitialRepo()

            //Reset connection
            ApiPersistenceFactory.set(null)
            val modify = repo.getFeatureRelease(8)!!.releases.nodes.values.first()
            val r = Release(modify.id, modify.release_type, modify.release_link, modify.release_name, modify.timestamp, LocalDateTime.now(), modify.binaries, modify.download_count, modify.vendor, modify.version_data)
            repo = repo.addRelease(8, r)

            ApiPersistenceFactory.get().updateAllRepos(repo)
            APIDataStore.loadDataFromDb()
        }
        AdoptRepositoryFactory.adoptRepository = AdoptRepositoryImpl
        V3Updater().run(false)
        Awaitility.await().atMost(Long.MAX_VALUE, TimeUnit.NANOSECONDS).until({ 4 === 5 })
    }
}