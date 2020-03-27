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

    //TODO urgent remove me when we are ready to go live on debug images
    private val EXCLUDED = listOf("debugimage")

    suspend fun toBinaryList(assets: List<GHAsset>, metadata: Map<GHAsset, GHMetaData>): List<Binary> {
        // probably whitelist rather than black list
        return assets
                .filter(this::isArchive)
                .filter { asset -> EXCLUDED.all { excluded -> !asset.name.contains(excluded) } }
                .map { asset -> assetToBinaryAsync(asset, metadata, assets) }
                .mapNotNull { it.await() }
    }

    private fun assetToBinaryAsync(
            asset: GHAsset,
            metadata: Map<GHAsset, GHMetaData>,
            assets: List<GHAsset>
    ): Deferred<Binary?> {
        return GlobalScope.async {
            try {
                val downloadCount = asset.downloadCount
                val updatedAt = getUpdatedTime(asset)

                val binaryMetadata = metadata[asset]

                val heapSize = getEnumFromFileName(asset.name, HeapSize.values(), HeapSize.normal)

                val installer = getInstaller(asset, assets)
                val pack = getPackage(assets, asset, binaryMetadata)


                if (binaryMetadata != null) {
                    return@async binaryFromMetadata(
                            binaryMetadata,
                            pack,
                            downloadCount,
                            updatedAt,
                            installer,
                            heapSize
                    )
                } else {
                    return@async binaryFromName(asset, pack, downloadCount, updatedAt, installer, heapSize)
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to fetch binary ${asset.name}", e)
                return@async null
            }
        }
    }

    private suspend fun getPackage(assets: List<GHAsset>, asset: GHAsset, binaryMetadata: GHMetaData?): Package {
        val binaryName = asset.name
        val binaryLink = asset.downloadUrl
        val binarySize = asset.size
        val binaryChecksumLink = getCheckSumLink(assets, binaryName)
        val binaryChecksum: String?

        binaryChecksum = if (binaryMetadata != null && binaryMetadata.sha256.isNotEmpty()) {
            binaryMetadata.sha256
        } else {
            getChecksum(binaryChecksumLink)
        }

        return Package(binaryName, binaryLink, binarySize, binaryChecksum, binaryChecksumLink, asset.downloadCount)
    }

    private suspend fun getInstaller(ghAsset: GHAsset, assets: List<GHAsset>): Installer? {

        val nameWithoutExtension =
                BINARY_ASSET_WHITELIST.fold(ghAsset.name, { assetName, extension -> assetName.replace(extension, "") })

        val installer = assets
                .filter { it.name.startsWith(nameWithoutExtension) }
                .firstOrNull { asset ->
                    INSTALLER_EXTENSIONS.any { asset.name.endsWith(it) }
                }

        return if (installer == null) {
            null
        } else {
            val link = getCheckSumLink(assets, installer.name)
            var checksum: String? = null
            if (link != null) {
                checksum = getChecksum(link)
            }

            Installer(installer.name, installer.downloadUrl, installer.size, checksum, link, installer.downloadCount)
        }
    }

    private fun getCheckSumLink(assets: List<GHAsset>, binary_name: String): String? {
        val nameWithoutExtension = removeExtensionFromName(binary_name)

        return assets
                .firstOrNull { asset ->
                    asset.name == "${binary_name}.sha256.txt" ||
                            binary_name.split(".")[0] + ".sha256.txt" == asset.name ||
                            "${nameWithoutExtension}.sha256.txt" == asset.name
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
            binaryMetadata: GHMetaData, pack: Package, download_count: Long, updated_at: ZonedDateTime,
            installer: Installer?, heap_size: HeapSize
    ): Binary {

        //github metadata has concept of hotspot-jfr split this into
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

                val checksum = CachedGithubHtmlClient.getUrl(binary_checksum_link);
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