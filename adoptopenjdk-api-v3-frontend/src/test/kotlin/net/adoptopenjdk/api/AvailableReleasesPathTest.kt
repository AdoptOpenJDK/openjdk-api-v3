package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.ReleaseType
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class AvailableReleasesPathTest : BaseTest() {

    @BeforeEach
    fun initDB() {
        runBlocking {
            val repo = AdoptReposBuilder.build(APIDataStore.variants.versions)

            val releases = repo.allReleases.getReleases()
                .filter { it.version_data.major < 13 || it.version_data.major == 13 && it.release_type == ReleaseType.ea }
                .groupBy { it.version_data.major }
                .toMap()
                .map { FeatureRelease(it.key, Releases(it.value)) }

            // Reset connection
            ApiPersistenceFactory.set(null)
            ApiPersistenceFactory.get().updateAllRepos(AdoptRepos(releases))
            APIDataStore.loadDataFromDb()
        }
    }

    @Test
    fun availableReleases() {
        RestAssured.given()
            .`when`()
            .get("/v3/info/available_releases")
            .then()
            .statusCode(200)
    }

    @Test
    fun availableVersionsIsCorrect() {
        check { releaseInfo ->
            releaseInfo.available_releases.contentEquals(arrayOf(8, 9, 10, 11, 12))
        }
    }

    @Test
    fun availableLtsIsCorrect() {
        check { releaseInfo ->
            releaseInfo.available_lts_releases.contentEquals(arrayOf(8, 11))
        }
    }

    @Test
    fun mostRecentLtsIsCorrect() {
        check { releaseInfo ->
            releaseInfo.most_recent_lts == 11
        }
    }

    @Test
    fun mostRecentFeatureReleaseIsCorrect() {
        check { releaseInfo ->
            releaseInfo.most_recent_feature_release == 12
        }
    }

    @Test
    fun mostRecentFeatureVersionIsCorrect() {
        check { releaseInfo ->
            releaseInfo.most_recent_feature_version == 13
        }
    }

    private fun check(matcher: (ReleaseInfo) -> Boolean) {
        RestAssured.given()
            .`when`()
            .get("/v3/info/available_releases")
            .then()
            .body(object : TypeSafeMatcher<String>() {

                override fun describeTo(description: Description?) {
                    description!!.appendText("json")
                }

                override fun matchesSafely(p0: String?): Boolean {
                    val releaseInfo = JsonMapper.mapper.readValue(p0, ReleaseInfo::class.java)
                    return matcher(releaseInfo)
                }
            })
    }
}
