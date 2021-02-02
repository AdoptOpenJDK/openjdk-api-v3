package net.adoptopenjdk.api.v3.dataSources

import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.models.ReleaseInfo
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Variants


object ReleaseVersionResolver {

    private const val VERSION_FILE_URL = "https://raw.githubusercontent.com/openjdk/jdk/master/make/autoconf/version-numbers"

    private suspend fun getTipVersion(): Int? {
        val versionFile = UpdaterHtmlClientFactory.client.get(VERSION_FILE_URL)

        return if (versionFile != null) {
            Regex(""".*DEFAULT_VERSION_FEATURE=(?<num>\d+).*""", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
                .matchEntire(versionFile)?.groups?.get("num")?.value?.toInt()
        } else {
            null
        }
    }

    private fun getObsoleteReleases(): Array<Int>? {
        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        val variants: Variants = UpdaterJsonMapper.mapper.readValue(variantData, Variants::class.java)

        val obsoleteVersions: MutableList<Int> = mutableListOf()

        variants.variants.forEach { variant ->
            if (variant.obsoleteRelease) {
                obsoleteVersions.add(variant.version)
            }
        }

        return if (obsoleteVersions.isNotEmpty()) {
            obsoleteVersions
                .distinct()
                .sorted()
                .toTypedArray()
        } else {
            return null
        }

    }

    suspend fun updateDbVersion(repo: AdoptRepos) {
        ApiPersistenceFactory.get().setReleaseInfo(formReleaseInfo(repo))
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

        val obsoleteReleases: Array<Int> = getObsoleteReleases() ?: arrayOf(9, 10)

        val mostRecentFeatureRelease: Int = availableReleases.last()

        val availableLtsReleases: Array<Int> = gaReleases
            .asSequence()
            .filter { APIDataStore.variants.ltsVersions.contains(it.version_data.major) }
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
            obsoleteReleases,
            availableLtsReleases,
            mostRecentLts,
            mostRecentFeatureRelease,
            mostRecentFeatureVersion,
            tip
        )
    }
}
