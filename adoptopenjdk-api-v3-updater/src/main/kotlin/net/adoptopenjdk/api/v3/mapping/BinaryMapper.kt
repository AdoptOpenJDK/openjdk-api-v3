package net.adoptopenjdk.api.v3.mapping

import java.time.ZonedDateTime
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.models.FileNameMatcher

abstract class BinaryMapper {

    companion object {
        val INSTALLER_EXTENSIONS = listOf("msi", "pkg")

        val BINARY_ASSET_WHITELIST: List<String> = listOf(".tar.gz", ".msi", ".pkg", ".zip", ".deb", ".rpm")
        val ARCHIVE_WHITELIST: List<String> = listOf(".tar.gz", ".zip")

        val BINARY_EXTENSIONS = ARCHIVE_WHITELIST.union(BINARY_ASSET_WHITELIST).union(INSTALLER_EXTENSIONS)
    }

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

    fun getUpdatedTime(asset: GHAsset): ZonedDateTime = ReleaseMapper.parseDate(asset.updatedAt)
}
