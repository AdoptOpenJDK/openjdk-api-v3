package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.testDoubles.AdoptRepositoryStub
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.GitHubId
import org.jboss.weld.junit5.auto.EnableAutoWeld
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@EnableAutoWeld
class AdoptReposBuilderTest : BaseTest() {

    companion object {
        private val stub = AdoptRepositoryStub()
        private val adoptReposBuilder: AdoptReposBuilder = AdoptReposBuilder(stub)
        private var before: AdoptRepos = stub.repo
        private var updated: AdoptRepos = runBlocking {
            adoptReposBuilder.incrementalUpdate(before)
        }
    }

    @Test
    fun removedReleaseIsRemovedWhenUpdated() {
        runBlocking {
            assertTrue { before.getFeatureRelease(8)!!.releases.hasReleaseId(GitHubId(stub.toRemove.id)) }
            assertTrue { !updated!!.getFeatureRelease(8)!!.releases.hasReleaseId(GitHubId(stub.toRemove.id)) }
            assertTrue { updated != before }
        }
    }

    @Test
    fun addReleaseIsAddWhenUpdated() {
        runBlocking {
            assertTrue { !before.getFeatureRelease(8)!!.releases.hasReleaseId(GitHubId("foo")) }
            assertTrue { updated!!.getFeatureRelease(8)!!.releases.getReleases().contains(AdoptRepositoryStub.toAdd) }
            assertTrue { updated != before }
        }
    }

    @Test
    fun releaseLessThan10MinOldIsNotUpdated() {
        runBlocking {
            assertTrue { !updated!!.getFeatureRelease(8)!!.releases.hasReleaseId(GitHubId("young")) }
        }
    }

    @Test
    fun updatedReleaseIsUpdatedWhenUpdated() {
        runBlocking {
            assertTrue { before.getFeatureRelease(8)!!.releases.getReleases().contains(stub.originaToUpdate) }
            assertTrue { !updated!!.getFeatureRelease(8)!!.releases.getReleases().contains(stub.originaToUpdate) }
            assertTrue { updated!!.getFeatureRelease(8)!!.releases.getReleases().contains(stub.toUpdate) }
            assertTrue { updated != before }
        }
    }

    @Test
    fun updatedReleaseIsNotUpdatedWhenThingsDontChange() {
        runBlocking {

            val updated2 = runBlocking {
                adoptReposBuilder.incrementalUpdate(before)
            }
            val updated3 = runBlocking {
                adoptReposBuilder.incrementalUpdate(before)
            }

            assertTrue { updated == updated2 }
            assertTrue { updated2 == updated3 }
        }
    }
}
