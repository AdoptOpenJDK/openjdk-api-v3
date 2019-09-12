package net.adoptopenjdk.api.v3.mapping.upstream

import net.adoptopenjdk.api.v3.dataSources.github.VersionParser
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.mapping.ReleaseMapper
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object UpstreamReleaseMapper : ReleaseMapper() {
    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    override suspend fun toAdoptRelease(release: GHRelease): Release {
        val release_type: ReleaseType = if (release.name.contains(" GA ")) ReleaseType.ga else ReleaseType.ea

        val release_link = release.url
        val release_name = release.name
        val timestamp = parseDate(release.publishedAt)
        val updatedAt = parseDate(release.updatedAt)
        val download_count = 1
        val vendor = Vendor.openjdk

        val versionData = getVersionData(release_name)

        LOGGER.info("Getting binaries ${release_name}")
        val binaries = UpstreamBinaryMapper.toBinaryList(release.releaseAssets.assets)
        LOGGER.info("Done Getting binaries ${release_name}")

        return Release(release.id, release_type, release_link, release_name, timestamp, updatedAt, binaries, download_count, vendor, versionData)
    }

    private fun getVersionData(release_name: String): VersionData {
        return VersionParser().parse(release_name)

    }

}