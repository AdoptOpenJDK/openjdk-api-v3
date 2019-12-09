package net.adoptopenjdk.api.v3.mapping.upstream

import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.SourcePackage
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import net.adoptopenjdk.api.v3.parser.FailedToParse
import net.adoptopenjdk.api.v3.parser.VersionParser
import org.slf4j.LoggerFactory
import java.net.URLDecoder
import java.nio.charset.Charset

object UpstreamReleaseMapper : ReleaseMapper() {
    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    override suspend fun toAdoptRelease(release: GHRelease): Release? {
        val release_type: ReleaseType = if (release.name.contains(" GA ")) ReleaseType.ga else ReleaseType.ea

        val release_link = release.url
        val release_name = release.name
        val timestamp = parseDate(release.publishedAt)
        val updatedAt = parseDate(release.updatedAt)
        val download_count = release.releaseAssets.assets.map { it.downloadCount }.sum()
        val vendor = Vendor.openjdk

        LOGGER.info("Getting binaries $release_name")
        val binaries = UpstreamBinaryMapper.toBinaryList(release.releaseAssets.assets)
        LOGGER.info("Done Getting binaries $release_name")

        try {
            val versionData: VersionData

            if (release_type == ReleaseType.ga && binaries.size > 0) {
                //Release names for ga do not have a full version name, so take it from the package
                val pack = binaries.get(0).`package`
                if (pack != null) {
                    versionData = getVersionData(URLDecoder.decode(pack.link, Charset.defaultCharset()))
                } else {
                    LOGGER.error("Failed to parse version for $release_name")
                    return null
                }
            } else {
                versionData = getVersionData(release_name)
            }

            val sourcePackage = getSourcePackage(release)

            return Release(release.id, release_type, release_link, release_name, timestamp, updatedAt, binaries.toTypedArray(), download_count, vendor, versionData, sourcePackage)
        } catch (e: FailedToParse) {
            LOGGER.error("Failed to parse $release_name")
            return null
        }
    }

    private fun getSourcePackage(release: GHRelease): SourcePackage? {
        return release.releaseAssets
                .assets
                .filter { it.name.endsWith("tar.gz") }
                .filter { it.name.contains("-sources") }
                .map { SourcePackage(it.name, it.downloadUrl, it.size) }
                .firstOrNull()
    }

    private fun getVersionData(release_name: String): VersionData {
        return VersionParser.parse(release_name)

    }

}