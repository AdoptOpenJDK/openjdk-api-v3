package net.adoptopenjdk.api

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.AdoptRepository
import net.adoptopenjdk.api.v3.ReleaseResult
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.GitHubId
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import org.jboss.weld.junit5.auto.AddPackages
import org.jboss.weld.junit5.auto.EnableAutoWeld
import org.jboss.weld.junit5.auto.ExcludeBean
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.annotation.Priority
import javax.enterprise.inject.Alternative
import javax.enterprise.inject.Produces
import javax.inject.Singleton
import kotlin.test.assertTrue

@Priority(1)
@Alternative
@Singleton
open class AdoptRepositoryStub : AdoptRepository {
    @Produces
    @ExcludeBean
    val repo = BaseTest.adoptRepos
    val toRemove = repo.getFeatureRelease(8)!!.releases.nodes.values.first()
    val originaToUpdate = repo.getFeatureRelease(8)!!.releases.nodes.values.take(2).last()

    val toUpdate = Release(
        originaToUpdate.id, ReleaseType.ga, "openjdk-8u", "jdk8u-2018-09-27-08-50",
        TimeSource.now().minusMinutes(20),
        TimeSource.now().minusMinutes(20),
        arrayOf(), 2, Vendor.adoptopenjdk,
        VersionData(8, 2, 3, "", 1, 4, "", "")
    )

    private val updated = repo
        .removeRelease(8, toRemove)
        .addRelease(8, toAdd)
        .addRelease(8, toAddYoung)
        .removeRelease(8, originaToUpdate)
        .addRelease(8, toUpdate)

    companion object {
        val toAdd = Release(
            "foo", ReleaseType.ga, "openjdk-8u", "jdk8u-2018-09-27-08-50",
            TimeSource.now().minusMinutes(20),
            TimeSource.now().minusMinutes(20),
            arrayOf(), 2, Vendor.adoptopenjdk,
            VersionData(8, 2, 3, "", 1, 4, "", "")
        )

        val toAddYoung = Release(
            "young", ReleaseType.ga, "openjdk-8u", "jdk8u-2018-09-27-08-50",
            TimeSource.now(),
            TimeSource.now(), arrayOf(), 2, Vendor.adoptopenjdk,
            VersionData(8, 2, 3, "", 1, 4, "", "")
        )
    }

    override suspend fun getRelease(version: Int): FeatureRelease? {
        return updated.getFeatureRelease(version)
    }

    override suspend fun getSummary(version: Int): GHRepositorySummary {
        return GHSummaryTestDataGenerator.generateGHRepositorySummary(
            GHSummaryTestDataGenerator.generateGHRepository(
                AdoptRepos(listOf(updated.getFeatureRelease(version)!!))
            )
        )
    }

    override suspend fun getReleaseById(id: GitHubId): ReleaseResult? {
        return updated
            .allReleases
            .getReleases()
            .filter {
                GitHubId(it.id) == id
            }
            .map {
                ReleaseResult(listOf(it))
            }
            .firstOrNull()
    }
}

@EnableAutoWeld
@ExtendWith(MockKExtension::class)
@AddPackages(value = [AdoptRepositoryStub::class])
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
