package net.adoptopenjdk.api.v3.mapping.upstream

import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.mapping.BinaryMapper
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

    override suspend fun toAdoptRelease(release: GHRelease): List<Release>? {
        val release_type: ReleaseType = if (release.name.contains(" GA ")) ReleaseType.ga else ReleaseType.ea

        val releaseLink = release.url
        val releaseName = release.name
        val timestamp = parseDate(release.publishedAt)
        val updatedAt = parseDate(release.updatedAt)
        val downloadCount = release.releaseAssets.assets
                .filter { asset ->
                    BinaryMapper.BINARY_EXTENSIONS.any { asset.name.endsWith(it) }
                }
                .map { it.downloadCount }.sum()

        val vendor = Vendor.openjdk

        LOGGER.info("Getting binaries $releaseName")
        val binaries = UpstreamBinaryMapper.toBinaryList(release.releaseAssets.assets)
        LOGGER.info("Done Getting binaries $releaseName")

        try {
            val versionData: VersionData

            if (release_type == ReleaseType.ga && binaries.size > 0) {
                //Release names for ga do not have a full version name, so take it from the package
                val pack = binaries.get(0).`package`
                versionData = getVersionData(URLDecoder.decode(pack.link, Charset.defaultCharset()))
            } else {
                versionData = getVersionData(releaseName)
            }

            val sourcePackage = getSourcePackage(release)

            return listOf(Release(release.id.githubId, release_type, releaseLink, releaseName, timestamp, updatedAt, binaries.toTypedArray(), downloadCount, vendor, versionData, sourcePackage))
        } catch (e: FailedToParse) {
            LOGGER.error("Failed to parse $releaseName")
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