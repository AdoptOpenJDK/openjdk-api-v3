package net.adoptopenjdk.api.v3.dataSources.persitence

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.http.HttpClient
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Variants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseInfoFactory @Inject constructor(private val httpClient: HttpClient) {
    companion object {
        val VERSION_FILE_URL = "https://raw.githubusercontent.com/openjdk/jdk/master/make/autoconf/version-numbers"
    }

    fun formReleaseInfo(adoptRepos: AdoptRepos, variants: Variants): ReleaseInfo {
        return runBlocking {
            val gaReleases = adoptRepos
                .allReleases
                .getReleases()
                .filter { it.release_type == ReleaseType.ga }
                .toList()

            val availableReleases = gaReleases
                .map { it.version_data.major }
                .distinct()
                .sorted()
                .toList()
                .toTypedArray()
            val mostRecentFeatureRelease: Int = availableReleases.last()

            val availableLtsReleases: Array<Int> = gaReleases
                .filter { variants.ltsVersions.contains(it.version_data.major) }
                .map { it.version_data.major }
                .distinct()
                .sorted()
                .toList()
                .toTypedArray()
            val mostRecentLts = availableLtsReleases.last()

            val mostRecentFeatureVersion: Int = adoptRepos
                .allReleases
                .getReleases()
                .map { it.version_data.major }
                .distinct()
                .sorted()
                .last()

            val tipVersion = getTipVersion(mostRecentFeatureRelease)

            ReleaseInfo(
                availableReleases,
                availableLtsReleases,
                mostRecentLts,
                mostRecentFeatureRelease,
                mostRecentFeatureVersion,
                tipVersion
            )
        }
    }

    private suspend fun getTipVersion(mostRecentFeatureRelease: Int): Int {
        val versionFile = httpClient.get(VERSION_FILE_URL)
        val skaraVersion = if (versionFile != null) {
            Regex(""".*DEFAULT_VERSION_FEATURE=(?<num>\d+).*""", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
                .matchEntire(versionFile)?.groups?.get("num")?.value?.toInt()
        } else {
            null
        }

        val tipVersion = if (skaraVersion != null) {
            skaraVersion
        } else {
            mostRecentFeatureRelease
        }
        return tipVersion
    }
}
