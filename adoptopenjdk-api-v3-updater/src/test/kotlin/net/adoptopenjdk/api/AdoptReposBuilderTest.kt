package net.adoptopenjdk.api

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.testDoubles.AdoptRepositoryStub
import net.adoptopenjdk.api.testDoubles.InMemoryApiPersistence
import net.adoptopenjdk.api.testDoubles.InMemoryInternalDbStore
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.GitHubId
import org.jboss.weld.junit5.auto.AddPackages
import org.jboss.weld.junit5.auto.EnableAutoWeld
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertTrue

@EnableAutoWeld
@ExtendWith(MockKExtension::class)
@AddPackages(value = [InMemoryApiPersistence::class, InMemoryInternalDbStore::class, AdoptRepositoryStub::class, AdoptReposBuilder::class])
class AdoptReposBuilderTest : BaseTest() {

    private var updated: AdoptRepos? = null

    @BeforeEach
    fun runUpdate(repo: AdoptRepos, adoptReposBuilder: AdoptReposBuilder) {
        if (updated == null) {
            updated = runBlocking {
                adoptReposBuilder.incrementalUpdate(repo)
            }
        }
    }

    @Test
    fun removedReleaseIsRemovedWhenUpdated(repo: AdoptRepos, adoptReposBuilder: AdoptReposBuilder, stub: AdoptRepositoryStub) {
        runBlocking {
            assertTrue { repo.getFeatureRelease(8)!!.releases.hasReleaseId(GitHubId(stub.toRemove.id)) }
            assertTrue { !updated!!.getFeatureRelease(8)!!.releases.hasReleaseId(GitHubId(stub.toRemove.id)) }
            assertTrue { updated != repo }
        }
    }

    @Test
    fun addReleaseIsAddWhenUpdated(repo: AdoptRepos) {
        runBlocking {
            assertTrue { !repo.getFeatureRelease(8)!!.releases.hasReleaseId(GitHubId("foo")) }
            assertTrue { updated!!.getFeatureRelease(8)!!.releases.getReleases().contains(AdoptRepositoryStub.toAdd) }
            assertTrue { updated != repo }
        }
    }

    @Test
    fun releaseLessThan10MinOldIsNotUpdated(repo: AdoptRepos, adoptReposBuilder: AdoptReposBuilder) {
        runBlocking {
            assertTrue { !updated!!.getFeatureRelease(8)!!.releases.hasReleaseId(GitHubId("young")) }
        }
    }

    @Test
    fun updatedReleaseIsUpdatedWhenUpdated(repo: AdoptRepos, adoptReposBuilder: AdoptReposBuilder, stub: AdoptRepositoryStub) {
        runBlocking {
            assertTrue { repo.getFeatureRelease(8)!!.releases.getReleases().contains(stub.originaToUpdate) }
            assertTrue { !updated!!.getFeatureRelease(8)!!.releases.getReleases().contains(stub.originaToUpdate) }
            assertTrue { updated!!.getFeatureRelease(8)!!.releases.getReleases().contains(stub.toUpdate) }
            assertTrue { updated != repo }
        }
    }

    @Test
    fun updatedReleaseIsNotUpdatedWhenThingsDontChange(repo: AdoptRepos, adoptReposBuilder: AdoptReposBuilder) {
        runBlocking {

            val updated2 = runBlocking {
                adoptReposBuilder.incrementalUpdate(repo)
            }
            val updated3 = runBlocking {
                adoptReposBuilder.incrementalUpdate(repo)
            }

            assertTrue { updated == updated2 }
            assertTrue { updated2 == updated3 }
        }
    }
}
