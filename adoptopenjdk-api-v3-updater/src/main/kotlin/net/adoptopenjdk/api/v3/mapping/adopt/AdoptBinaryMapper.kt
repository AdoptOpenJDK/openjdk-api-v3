package net.adoptopenjdk.api.v3.mapping.adopt

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHMetaData
import net.adoptopenjdk.api.v3.dataSources.mongo.CachedGithubHtmlClient
import net.adoptopenjdk.api.v3.mapping.BinaryMapper
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.Binary
import net.adoptopenjdk.api.v3.models.HeapSize
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.Installer
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.Package
import net.adoptopenjdk.api.v3.models.Project
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

object AdoptBinaryMapper : BinaryMapper() {

    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)
    private const val HOTSPOT_JFR = "hotspot-jfr"

    private val EXCLUDED = listOf<String>()

    suspend fun toBinaryList(assets: List<GHAsset>, fullAssetList: List<GHAsset>, metadata: Map<GHAsset, GHMetaData>): List<Binary> {
        // probably whitelist rather than black list
        return assets
            .filter(this::isArchive)
            .filter { asset -> EXCLUDED.all { excluded -> !asset.name.contains(excluded) } }
            .map { asset -> assetToBinaryAsync(asset, metadata, fullAssetList) }
            .mapNotNull { it.await() }
    }

    private fun assetToBinaryAsync(
        asset: GHAsset,
        metadata: Map<GHAsset, GHMetaData>,
        fullAssetList: List<GHAsset>
    ): Deferred<Binary?> {
        return GlobalScope.async {
            try {
                val updatedAt = getUpdatedTime(asset)

                val binaryMetadata = metadata[asset]

                val heapSize = getEnumFromFileName(asset.name, HeapSize.values(), HeapSize.normal)

                val installer = getInstaller(asset, fullAssetList)
                val `package` = getPackage(fullAssetList, asset, binaryMetadata)
                val downloadCount = `package`.download_count + (installer?.download_count ?: 0)

                if (binaryMetadata != null) {
                    return@async binaryFromMetadata(
                        binaryMetadata,
                        `package`,
                        downloadCount,
                        updatedAt,
                        installer,
                        heapSize
                    )
                } else {
                    return@async binaryFromName(asset, `package`, downloadCount, updatedAt, installer, heapSize)
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to fetch binary ${asset.name}", e)
                return@async null
            }
        }
    }

    private suspend fun getPackage(fullAssetList: List<GHAsset>, asset: GHAsset, binaryMetadata: GHMetaData?): Package {
        val binaryName = asset.name
        val binaryLink = asset.downloadUrl
        val binarySize = asset.size
        val binaryChecksumLink = getCheckSumLink(fullAssetList, binaryName)
        val binaryChecksum: String?

        binaryChecksum = if (binaryMetadata != null && binaryMetadata.sha256.isNotEmpty()) {
            binaryMetadata.sha256
        } else {
            getChecksum(binaryChecksumLink)
        }

        val metadataLink = getMetadataLink(fullAssetList, binaryName)

        return Package(
            binaryName,
            binaryLink,
            binarySize,
            binaryChecksum,
            binaryChecksumLink,
            asset.downloadCount,
            signature_link = null,
            metadata_link = metadataLink
        )
    }

    private suspend fun getInstaller(ghAsset: GHAsset, fullAssetList: List<GHAsset>): Installer? {

        val nameWithoutExtension =
            BINARY_ASSET_WHITELIST.fold(ghAsset.name, { assetName, extension -> assetName.replace(extension, "") })

        val installer = fullAssetList
            .filter { it.name.startsWith(nameWithoutExtension) }
            .firstOrNull { asset ->
                INSTALLER_EXTENSIONS.any { asset.name.endsWith(it) }
            }

        return if (installer == null) {
            null
        } else {
            val checkSumLink = getCheckSumLink(fullAssetList, installer.name)
            var checksum: String? = null
            if (checkSumLink != null) {
                checksum = getChecksum(checkSumLink)
            }

            val metadataLink = getMetadataLink(fullAssetList, installer.name)

            Installer(
                installer.name,
                installer.downloadUrl,
                installer.size,
                checksum,
                checkSumLink,
                installer.downloadCount,
                signature_link = null,
                metadata_link = metadataLink
            )
        }
    }

    private fun getCheckSumLink(fullAssetList: List<GHAsset>, binary_name: String): String? {
        val nameWithoutExtension = removeExtensionFromName(binary_name)

        return fullAssetList
            .firstOrNull { asset ->
                asset.name == "$binary_name.sha256.txt" ||
                    asset.name == binary_name.split(".")[0] + ".sha256.txt" ||
                    asset.name == "$nameWithoutExtension.sha256.txt"
            }?.downloadUrl
    }

    private fun getMetadataLink(fullAssetList: List<GHAsset>, binary_name: String): String? {
        val nameWithoutExtension = removeExtensionFromName(binary_name)

        return fullAssetList
            .firstOrNull { asset ->
                asset.name == "$binary_name.json" ||
                    asset.name == binary_name.split(".")[0] + ".json" ||
                    asset.name == "$nameWithoutExtension.json"
            }?.downloadUrl
    }

    private fun removeExtensionFromName(binary_name: String): String {
        return BINARY_ASSET_WHITELIST.foldRight(binary_name, { extension, name -> name.removeSuffix(extension) })
    }

    private fun isArchive(asset: GHAsset) = ARCHIVE_WHITELIST.any { asset.name.endsWith(it) }

    private fun binaryFromName(
        asset: GHAsset,
        pack: Package,
        download_count: Long,
        updated_at: ZonedDateTime,
        installer: Installer?,
        heap_size: HeapSize
    ): Binary {
        val scmRef = null
        val os = getEnumFromFileName(asset.name, OperatingSystem.values())
        val architecture = getEnumFromFileName(asset.name, Architecture.values())
        val binaryType = getEnumFromFileName(asset.name, ImageType.values(), ImageType.jdk)
        val jvmImpl = getEnumFromFileName(asset.name, JvmImpl.values(), JvmImpl.hotspot)
        val project = getEnumFromFileName(asset.name, Project.values(), Project.jdk)

        return Binary(
            pack,
            download_count,
            updated_at,
            scmRef,
            installer,
            heap_size,
            os,
            architecture,
            binaryType,
            jvmImpl,
            project
        )
    }

    private fun binaryFromMetadata(
        binaryMetadata: GHMetaData,
        pack: Package,
        download_count: Long,
        updated_at: ZonedDateTime,
        installer: Installer?,
        heap_size: HeapSize
    ): Binary {

        // github metadata has concept of hotspot-jfr split this into
        val variant = parseJvmImpl(binaryMetadata)
        val project = parseProject(binaryMetadata)

        return Binary(
            pack,
            download_count,
            updated_at,
            binaryMetadata.scmRef,
            installer,
            heap_size,
            binaryMetadata.os,
            binaryMetadata.arch,
            binaryMetadata.binary_type,
            variant,
            project
        )
    }

    private fun parseProject(binaryMetadata: GHMetaData): Project {
        return if (binaryMetadata.variant == HOTSPOT_JFR) {
            Project.jfr
        } else {
            Project.jdk
        }
    }

    private fun parseJvmImpl(binaryMetadata: GHMetaData): JvmImpl {
        return if (binaryMetadata.variant == HOTSPOT_JFR) {
            JvmImpl.hotspot
        } else {
            JvmImpl.valueOf(binaryMetadata.variant)
        }
    }

    private suspend fun getChecksum(binary_checksum_link: String?): String? {
        try {
            if (!(binary_checksum_link == null || binary_checksum_link.isEmpty())) {
                LOGGER.debug("Pulling checksum for $binary_checksum_link")

                val checksum = CachedGithubHtmlClient.getUrl(binary_checksum_link)
                if (checksum != null) {
                    val tokens = checksum.split(" ")
                    if (tokens.size > 1) {
                        return tokens[0]
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.warn("Failed to fetch checksum $binary_checksum_link", e)
        }
        return null
    }
}
