package net.adoptopenjdk.api.v3.mapping.adopt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.adoptopenjdk.api.v3.dataSources.UpdaterJsonMapper
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAssets
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHMetaData
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.mongo.CachedGithubHtmlClient
import net.adoptopenjdk.api.v3.mapping.BinaryMapper
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import net.adoptopenjdk.api.v3.parser.FailedToParse
import net.adoptopenjdk.api.v3.parser.VersionParser
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.util.*
import java.util.regex.Pattern

object AdoptReleaseMapper : ReleaseMapper() {
    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    override suspend fun toAdoptRelease(release: GHRelease): List<Release> {
        val releaseType: ReleaseType = formReleaseType(release)

        val releaseLink = release.url
        val releaseName = release.name
        val timestamp = parseDate(release.publishedAt)
        val updatedAt = parseDate(release.updatedAt)
        val vendor = Vendor.adoptopenjdk

        val metadata = getMetadata(release.releaseAssets)

        try {
            val groupedByVersion = metadata
                .entries
                .groupBy {
                    val version = it.value.version
                    "${version.major}.${version.minor}.${version.security}.${version.build}.${version.adopt_build_number}"
                }

            return groupedByVersion
                .entries
                .map { grouped ->
                    val version = grouped.value.sortedBy { it.value.version.toApiVersion() }
                        .last().value.version.toApiVersion()

                    val assets = grouped.value.map { it.key }
                    val id = generateIdForSplitRelease(version, release)

                    toRelease(releaseName, assets, metadata, id, releaseType, releaseLink, timestamp, updatedAt, vendor, version, release.releaseAssets.assets)
                }
                .ifEmpty {
                    try {
                        // if we have no metadata resort to parsing release names
                        val version = parseVersionInfo(release, releaseName)
                        val assets = release.releaseAssets.assets
                        val id = release.id.githubId

                        return@ifEmpty listOf(toRelease(releaseName, assets, metadata, id, releaseType, releaseLink, timestamp, updatedAt, vendor, version, assets))
                    } catch (e: Exception) {
                        throw Exception("Failed to parse version $releaseName")
                    }
                }
        } catch (e: FailedToParse) {
            LOGGER.error("Failed to parse $releaseName")
            throw e
        }
    }

    private fun generateIdForSplitRelease(version: VersionData, release: GHRelease): String {
        // using a shortend hash as a suffix to keep id short, probability of clash still very low
        val suffix = Base64
            .getEncoder()
            .encodeToString(MessageDigest
                .getInstance("SHA-1")
                .digest(version.semver.toByteArray())
                .copyOfRange(0, 10)
            )

        return release.id.githubId + "." + suffix
    }

    private suspend fun toRelease(
        releaseName: String,
        assets: List<GHAsset>,
        metadata: Map<GHAsset, GHMetaData>,
        id: String,
        release_type: ReleaseType,
        releaseLink: String,
        timestamp: ZonedDateTime,
        updatedAt: ZonedDateTime,
        vendor: Vendor,
        version: VersionData,
        fullAssetList: List<GHAsset>
    ): Release {
        LOGGER.info("Getting binaries $releaseName")
        val binaries = AdoptBinaryMapper.toBinaryList(assets, fullAssetList, metadata)
        LOGGER.info("Done Getting binaries $releaseName")

        val downloadCount = assets
            .filter { asset ->
                BinaryMapper.BINARY_EXTENSIONS.any { asset.name.endsWith(it) }
            }
            .map { it.downloadCount }.sum()

        return Release(id, release_type, releaseLink, releaseName, timestamp, updatedAt, binaries.toTypedArray(), downloadCount, vendor, version)
    }

    private fun formReleaseType(release: GHRelease): ReleaseType {
        // TODO fix me before the year 2100
        val dateMatcher = """.*(20[0-9]{2}-[0-9]{2}-[0-9]{2}|20[0-9]{6}).*"""
        val hasDate = Pattern.compile(dateMatcher).matcher(release.name)

        return if (release.url.matches(Regex(".*/openjdk[0-9]+-binaries/.*"))) {
            // Can trust isPrerelease from -binaries repos
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
    }

    private fun parseVersionInfo(release: GHRelease, release_name: String): VersionData {
        return try {
            VersionParser.parse(release_name)
        } catch (e: FailedToParse) {
            try {
                getFeatureVersion(release)
            } catch (e: Exception) {
                LOGGER.warn("Failed to parse ${release.name}")
                throw e
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
                return withContext(Dispatchers.IO) {
                    val metadata = UpdaterJsonMapper.mapper.readValue(metadataString, GHMetaData::class.java)
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
