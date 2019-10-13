package net.adoptopenjdk.api.v3.mapping.adopt

import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.github.VersionParser
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAssets
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHMetaData
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.regex.Pattern

object AdoptReleaseMapper : ReleaseMapper() {
    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    override suspend fun toAdoptRelease(release: GHRelease): Release {
        //TODO fix me before the year 2100
        val dateMatcher = """.*(20[0-9]{2}-[0-9]{2}-[0-9]{2}|20[0-9]{6}).*"""
        val hasDate = Pattern.compile(dateMatcher).matcher(release.name)
        val release_type: ReleaseType = if (hasDate.matches()) ReleaseType.ea else ReleaseType.ga

        val releaseLink = release.url
        val releaseName = release.name
        val timestamp = parseDate(release.publishedAt)
        val updatedAt = parseDate(release.updatedAt)
        val download_count = release.releaseAssets.assets.map { it.downloadCount }.sum()
        val vendor = Vendor.adoptopenjdk


        val metadata = getMetadata(release.releaseAssets)

        val versionData = getVersionData(release, metadata, release_type, releaseName)

        LOGGER.info("Getting binaries ${releaseName}")
        val binaries = AdoptBinaryMapper.toBinaryList(release.releaseAssets.assets, metadata)
        LOGGER.info("Done Getting binaries ${releaseName}")

        return Release(release.id, release_type, releaseLink, releaseName, timestamp, updatedAt, binaries.toTypedArray(), download_count, vendor, versionData)
    }

    private fun getVersionData(release: GHRelease, metadata: Map<GHAsset, GHMetaData>, release_type: ReleaseType, release_name: String): VersionData {
        return metadata
                .values
                .map { it.version.toApiVersion() }
                .ifEmpty {
                    //if we have no metadata resort to parsing release names
                    parseVersionInfo(release, release_type, release_name)
                }
                .first()

    }

    private fun parseVersionInfo(release: GHRelease, release_type: ReleaseType, release_name: String): List<VersionData> {
        return if (release_type == ReleaseType.ga) {
            listOf(VersionParser().parse(release_name))
        } else {
            listOf(getFeatureVersion(release, release_name))
        }
    }

    private fun getMetadata(releaseAssets: GHAssets): Map<GHAsset, GHMetaData> {
        return releaseAssets
                .assets
                .filter { it.name.endsWith(".json") }
                .map { metadataAsset ->
                    pairUpBinaryAndMetadata(releaseAssets, metadataAsset)
                }
                .filterNotNull()
                .toMap()
    }

    private fun pairUpBinaryAndMetadata(releaseAssets: GHAssets, metadataAsset: GHAsset): Pair<GHAsset, GHMetaData>? {
        val binaryAsset = releaseAssets
                .assets
                .filter {
                    metadataAsset.name.startsWith(it.name)
                }
                .firstOrNull()

        val metadata = getMetadata(metadataAsset)
        return if (binaryAsset == null || metadata == null) {
            null
        } else {
            Pair(binaryAsset, metadata)
        }
    }

    private fun getMetadata(it: GHAsset): GHMetaData? {
        val request = HttpRequest.newBuilder()
                .uri(URI.create(it.downloadUrl))
                .build()

        val metadata = HttpClientFactory.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).get()

        if (metadata.statusCode() == 200 && metadata.body() != null) {
            try {
                return JsonMapper.mapper.readValue(metadata.body(), GHMetaData::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun getFeatureVersion(release: GHRelease, release_name: String): VersionData {
        val featureVersionMatcher = """.*/adoptopenjdk/openjdk(?<feature>[0-9]+).*"""
        val matched = Pattern.compile(featureVersionMatcher).matcher(release.resourcePath.toLowerCase())

        if (matched.matches()) {
            val featureNumber = matched.group("feature").toInt()
            return VersionData(featureNumber, 0, 0, null, 0, 0, null, "")
        } else {
            //TODO: Catch this sooner
            throw IllegalStateException("Failed to find feature version for ${release_name}")
        }
    }
}