package net.adoptopenjdk.api

import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.AdoptRepositoryFactory
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.models.GithubId
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import org.junit.jupiter.api.Test

class AdoptReposBuilderTest : BaseTest() {

    @Test
    fun removedReleaseIsRemovedWhenUpdated() {
        runBlocking {
            val repo = getInitialRepo()
            val toRemove = repo.getFeatureRelease(8)!!.releases.nodes.values.first()
            val removedRepo = repo.removeRelease(8, toRemove)

            AdoptRepositoryFactory.setAdoptRepository(MockRepository(removedRepo))

            val updated = AdoptReposBuilder.incrementalUpdate(repo)

            assertTrue { repo.getFeatureRelease(8)!!.releases.hasReleaseId(GithubId(toRemove.id)) }
            assertTrue { !updated.getFeatureRelease(8)!!.releases.hasReleaseId(GithubId(toRemove.id)) }
            assertTrue { updated != repo }
        }
    }

    @Test
    fun addReleaseIsAddWhenUpdated() {
        runBlocking {
            val repo = getInitialRepo()
            val toAdd = Release("foo", ReleaseType.ga, "a", "b",
                    TimeSource.now().minusMinutes(20),
                    TimeSource.now().minusMinutes(20),
                    arrayOf(), 2, Vendor.adoptopenjdk,
                    VersionData(1, 2, 3, "", 1, 4, "", ""))

            val removedRepo = repo.addRelease(8, toAdd)

            AdoptRepositoryFactory.setAdoptRepository(MockRepository(removedRepo))

            val updated = AdoptReposBuilder.incrementalUpdate(repo)

            assertTrue { !repo.getFeatureRelease(8)!!.releases.hasReleaseId(GithubId(toAdd.id)) }
            assertTrue { updated.getFeatureRelease(8)!!.releases.getReleases().contains(toAdd) }
            assertTrue { updated != repo }
        }
    }

    @Test
    fun releaseLessThan10MinOldIsNotUpdated() {
        runBlocking {
            val repo = getInitialRepo()

            val toAdd = Release("foo", ReleaseType.ga, "a", "b",
                    TimeSource.now(),
                    TimeSource.now(), arrayOf(), 2, Vendor.adoptopenjdk,
                    VersionData(1, 2, 3, "", 1, 4, "", ""))

            val removedRepo = repo.addRelease(8, toAdd)

            AdoptRepositoryFactory.setAdoptRepository(MockRepository(removedRepo))

            val updated = AdoptReposBuilder.incrementalUpdate(repo)

            assertTrue { updated == repo }
        }
    }

    @Test
    fun updatedReleaseIsUpdatedWhenUpdated() {
        runBlocking {
            val repo = getInitialRepo()

            val original = repo.getFeatureRelease(8)!!.releases.nodes.values.first()

            val toUpdate = Release(original.id, ReleaseType.ga, "a", "b",
                    TimeSource.now(),
                    TimeSource.now().minusMinutes(20),
                    arrayOf(), 2, Vendor.adoptopenjdk,
                    VersionData(1, 2, 3, "", 1, 4, "", ""))

            val updatedRepo = repo.removeRelease(8, original) // .addRelease(8, toUpdate)

            AdoptRepositoryFactory.setAdoptRepository(MockRepository(updatedRepo))

            val updated = AdoptReposBuilder.incrementalUpdate(repo)

            assertTrue { repo.getFeatureRelease(8)!!.releases.getReleases().contains(original) }
            assertTrue { !updated.getFeatureRelease(8)!!.releases.getReleases().contains(original) }
            assertTrue { !updated.getFeatureRelease(8)!!.releases.getReleases().contains(toUpdate) }
            assertTrue { updated != repo }
        }
    }

    @Test
    fun updatedReleaseIsNotUpdatedWhenThingsDontChange() {
        runBlocking {
            val repo = getInitialRepo()
            val updated = AdoptReposBuilder.incrementalUpdate(repo)

            assertTrue { updated == repo }
        }
    }
}
