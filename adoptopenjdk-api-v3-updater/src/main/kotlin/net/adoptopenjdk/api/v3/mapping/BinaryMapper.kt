package net.adoptopenjdk.api.v3.mapping

import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.models.FileNameMatcher
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

abstract class BinaryMapper {

    val INSTALLER_EXTENSIONS = listOf("msi", "pkg")

    val BINARY_ASSET_WHITELIST: List<String> = listOf(".tar.gz", ".msi", ".pkg", ".zip", ".deb", ".rpm")
    val ARCHIVE_WHITELIST: List<String> = listOf(".tar.gz", ".zip")

    fun <T : FileNameMatcher> getEnumFromFileName(fileName: String, values: Array<T>, default: T? = null): T {

        val matched = values
                .filter { it.matchesFile(fileName) }
                .toList()

        if (matched.size != 1) {
            if (default != null) {
                return default
            }

            throw IllegalArgumentException("cannot determine ${values.get(0).javaClass.name} of asset $fileName")
        } else {
            return matched.get(0)
        }
    }


    fun getUpdatedTime(asset: GHAsset): LocalDateTime =
            Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(asset.updatedAt)).atZone(ZoneId.of("UTC")).toLocalDateTime()
}