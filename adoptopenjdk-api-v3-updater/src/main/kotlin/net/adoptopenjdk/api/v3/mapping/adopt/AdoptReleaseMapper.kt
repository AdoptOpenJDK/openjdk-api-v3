package net.adoptopenjdk.api.v3.mapping.adopt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAssets
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHMetaData
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.mongo.CachedGithubHtmlClient
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import net.adoptopenjdk.api.v3.parser.FailedToParse
import net.adoptopenjdk.api.v3.parser.VersionParser
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

object AdoptReleaseMapper : ReleaseMapper() {
    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    override suspend fun toAdoptRelease(release: GHRelease): Release? {
        val release_type: ReleaseType = formReleaseType(release)

        val releaseLink = release.url
        val releaseName = release.name
        val timestamp = parseDate(release.publishedAt)
        val updatedAt = parseDate(release.updatedAt)
        val downloadCount = release.releaseAssets.assets.map { it.downloadCount }.sum()
        val vendor = Vendor.adoptopenjdk


        val metadata = getMetadata(release.releaseAssets)

        try {
            val versionData = getVersionData(release, metadata, releaseName)
            if (versionData == null) return null

            LOGGER.info("Getting binaries $releaseName")
            val binaries = AdoptBinaryMapper.toBinaryList(release.releaseAssets.assets, metadata)
            LOGGER.info("Done Getting binaries $releaseName")

            return Release(release.id, release_type, releaseLink, releaseName, timestamp, updatedAt, binaries.toTypedArray(), downloadCount, vendor, versionData)
        } catch (e: FailedToParse) {
            LOGGER.error("Failed to parse $releaseName")
            return null
        }
    }

    private fun formReleaseType(release: GHRelease): ReleaseType {
        //TODO fix me before the year 2100
        val dateMatcher = """.*(20[0-9]{2}-[0-9]{2}-[0-9]{2}|20[0-9]{6}).*"""
        val hasDate = Pattern.compile(dateMatcher).matcher(release.name)
        val release_type: ReleaseType =
                if (release.url.matches(Regex(".*/openjdk[0-9]+-binaries/.*"))) {
                    //Can trust isPrerelease from -binaries repos
                    if (release.isPrerelease) {
                        ReleaseType.ea
                    } else {
                        ReleaseType.ga
                    }
                } else {
                    if (hasDate.matches()) {
                        ReleaseType.ea
                    } else {
                        ReleaseType.ga
                    }
                }
        return release_type
    }

    private fun getVersionData(release: GHRelease, metadata: Map<GHAsset, GHMetaData>, release_name: String): VersionData? {
        return metadata
                .values
                .map { it.version.toApiVersion() }
                .ifEmpty {
                    //if we have no metadata resort to parsing release names
                    parseVersionInfo(release, release_name)
                }
                .firstOrNull()

    }

    private fun parseVersionInfo(release: GHRelease, release_name: String): List<VersionData> {
        return try {
            listOf(VersionParser.parse(release_name))
        } catch (e: FailedToParse) {
            try {
                listOf(getFeatureVersion(release))
            } catch (e: Exception) {
                LOGGER.warn("Failed to parse ${release.name}")
                emptyList()
            }
        }
    }

    private suspend fun getMetadata(releaseAssets: GHAssets): Map<GHAsset, GHMetaData> {
        return releaseAssets
                .assets
                .filter { it.name.endsWith(".json") }
                .mapNotNull { metadataAsset ->
                    pairUpBinaryAndMetadata(releaseAssets, metadataAsset)
                }
                .toMap()
    }

    private suspend fun pairUpBinaryAndMetadata(releaseAssets: GHAssets, metadataAsset: GHAsset): Pair<GHAsset, GHMetaData>? {
        val binaryAsset = releaseAssets
                .assets
                .firstOrNull {
                    metadataAsset.name.startsWith(it.name)
                }

        val metadataString = CachedGithubHtmlClient.getUrl(metadataAsset.downloadUrl)
        if (binaryAsset != null && metadataString != null) {
            try {
                withContext(Dispatchers.IO) {
                    val metadata = JsonMapper.mapper.readValue(metadataString, GHMetaData::class.java)
                    return@withContext Pair(binaryAsset, metadata)
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to read metadata", e)
            }
        }
        return null
    }

    private fun getFeatureVersion(release: GHRelease): VersionData {
        return VersionParser.parse(release.name)
    }
}