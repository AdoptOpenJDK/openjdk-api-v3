package net.adoptopenjdk.api.v3.dataSources

import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.ReleaseType
import javax.inject.Inject

class ReleaseVersionResolver @Inject constructor(private var database: ApiPersistence) {

    private val VERSION_FILE_URL = "https://raw.githubusercontent.com/openjdk/jdk/master/make/autoconf/version-numbers"

    private suspend fun getTipVersion(): Int? {
        val versionFile = UpdaterHtmlClientFactory.client.get(VERSION_FILE_URL)

        return if (versionFile != null) {
            Regex(""".*DEFAULT_VERSION_FEATURE=(?<num>\d+).*""", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
                .matchEntire(versionFile)?.groups?.get("num")?.value?.toInt()
        } else {
            null
        }
    }

    suspend fun updateDbVersion(repo: AdoptRepos) {
        database.setReleaseInfo(formReleaseInfo(repo))
    }

    suspend fun formReleaseInfo(repo: AdoptRepos): ReleaseInfo {
        val gaReleases = repo
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
            .asSequence()
            .filter { VariantStore.variants.ltsVersions.contains(it.version_data.major) }
            .map { it.version_data.major }
            .distinct()
            .sorted()
            .toList()
            .toTypedArray()
        val mostRecentLts = availableLtsReleases.last()

        val mostRecentFeatureVersion: Int = repo
            .allReleases
            .getReleases()
            .map { it.version_data.major }
            .distinct()
            .sorted()
            .last()

        val tip = getTipVersion() ?: mostRecentFeatureVersion

        return ReleaseInfo(
            availableReleases,
            availableLtsReleases,
            mostRecentLts,
            mostRecentFeatureRelease,
            mostRecentFeatureVersion,
            tip
        )
    }
}
