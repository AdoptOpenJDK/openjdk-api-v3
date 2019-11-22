package net.adoptopenjdk.api.v3.mapping.upstream

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHAsset
import net.adoptopenjdk.api.v3.mapping.BinaryMapper
import net.adoptopenjdk.api.v3.mapping.adopt.AdoptBinaryMapper
import net.adoptopenjdk.api.v3.models.*
import org.slf4j.LoggerFactory

object UpstreamBinaryMapper : BinaryMapper() {

    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    private val EXCLUDES = listOf("sources", "debuginfo")

    suspend fun toBinaryList(assets: List<GHAsset>): List<Binary> {
        return assets
                .filter(this::isArchive)
                .filter({ !assetIsExcluded(it) })
                .map { asset -> assetToBinary(asset) }
                .map { binaryList -> binaryList.await() }
                .filterNotNull()
    }

    private fun assetIsExcluded(asset: GHAsset) = EXCLUDES.any({ exclude -> asset.name.contains(exclude) })

    private fun assetToBinary(asset: GHAsset): Deferred<Binary?> {
        return GlobalScope.async {
            try {
                //TODO add signature to Package
                val pack = Package(asset.name, asset.downloadUrl, asset.size, null, null)

                val os = getEnumFromFileName(asset.name, OperatingSystem.values())
                val architecture = getEnumFromFileName(asset.name, Architecture.values())
                val imageType = getEnumFromFileName(asset.name, ImageType.values())
                val updatedAt = getUpdatedTime(asset)

                Binary(
                        pack,
                        asset.downloadCount,
                        updatedAt,
                        null,
                        null,
                        HeapSize.normal,
                        os,
                        architecture,
                        imageType,
                        JvmImpl.hotspot,
                        Project.jdk
                )
            } catch (e: Exception) {
                LOGGER.error("Failed to parse binary data", e)
                return@async null
            }
        }
    }

    private fun isArchive(asset: GHAsset) =
            AdoptBinaryMapper.ARCHIVE_WHITELIST.filter { asset.name.endsWith(it) }.isNotEmpty()


}