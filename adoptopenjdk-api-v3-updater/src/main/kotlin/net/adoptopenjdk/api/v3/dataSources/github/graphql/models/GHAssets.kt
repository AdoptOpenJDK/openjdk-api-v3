package net.adoptopenjdk.api.v3.dataSources.github.graphql.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.models.*
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class GHAssets @JsonCreator constructor(
	@JsonProperty("nodes") val assets: List<GHAsset>,
	@JsonProperty("pageInfo") val pageInfo: PageInfo
) {

	companion object {
		@JvmStatic
		private val LOGGER = LoggerFactory.getLogger(this::class.java)
	}

	val BINARY_ASSET_WHITELIST: List<String> = listOf(".tar.gz", ".msi", ".pkg", ".zip", ".deb", ".rpm")
	val ARCHIVE_WHITELIST: List<String> = listOf(".tar.gz", ".zip")

	val INSTALLER_EXTENSIONS = listOf("msi", "pkg")

	suspend fun toBinaryList(metadata: Map<GHAsset, GHMetaData>): List<Binary> {
		// probably whitelist rather than black list
		return assets
			.filter(this::isArchive)
			.map { asset -> assetToBinary(asset, metadata, assets) }
			.map { binaryList -> binaryList.await() }
			.filterNotNull()
	}

	private fun assetToBinary(
		asset: GHAsset,
		metadata: Map<GHAsset, GHMetaData>,
		assets: List<GHAsset>
	): Deferred<Binary?> {
		return GlobalScope.async {
			try {
				val download_count = asset.downloadCount
				val updated_at = getUpdatedTime(asset)

				val binaryMetadata = metadata.get(asset)

				val heap_size = getEnumFromFileName(asset.name, HeapSize.values(), HeapSize.normal)

				val installer = getInstaller(asset, assets)
				val pack = getPackage(asset, binaryMetadata)


				if (binaryMetadata != null) {
					return@async binaryFromMetadata(
						binaryMetadata,
						pack,
						download_count,
						updated_at,
						installer,
						heap_size
					)
				} else {
					return@async binaryFromName(asset, pack, download_count, updated_at, installer, heap_size)
				}
			} catch (e: Exception) {
				return@async null
			}
		}
	}

	private fun getPackage(asset: GHAsset, binaryMetadata: GHMetaData?): Package {
		val binary_name = asset.name
		val binary_link = asset.downloadUrl
		val binary_size = asset.size
		val binary_checksum_link = getCheckSumLink(binary_name)
		val binary_checksum: String?

		if (binaryMetadata != null) {
			binary_checksum = binaryMetadata.sha256
		} else {
			binary_checksum = getChecksum(binary_checksum_link)
		}

		return Package(binary_name, binary_link, binary_size, binary_checksum, binary_checksum_link)
	}

	private fun getInstaller(asset: GHAsset, assets: List<GHAsset>): Installer? {

		val nameWithoutExtension =
			BINARY_ASSET_WHITELIST.fold(asset.name, { assetName, extension -> assetName.replace(extension, "") })

		val installer = assets
			.filter { it.name.startsWith(nameWithoutExtension) }
			.filter { asset ->
				INSTALLER_EXTENSIONS.any { asset.name.endsWith(it) }
			}
			.firstOrNull()

		if (installer == null) {
			return null
		} else {
			val link = getCheckSumLink(installer.name)
			var checksum: String? = null
			if (link != null) {
				checksum = getChecksum(link)
			}

			return Installer(installer.name, installer.downloadUrl, installer.size, checksum, link)
		}
	}

	private fun getUpdatedTime(asset: GHAsset) =
		Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(asset.updatedAt)).atZone(ZoneId.of("UTC")).toLocalDateTime()

	private fun getCheckSumLink(binary_name: String): String? {
		return assets
			.firstOrNull { asset ->
				asset.name.equals("${binary_name}.sha256.txt") ||
						(binary_name.split(".")[0] + ".sha256.txt").equals(asset.name)
			}
			?.downloadUrl
	}

	private fun isArchive(asset: GHAsset) =
		ARCHIVE_WHITELIST.filter { asset.name.endsWith(it) }.isNotEmpty()


	private fun binaryFromName(
		asset: GHAsset,
		pack: Package,
		download_count: Long,
		updated_at: LocalDateTime,
		installer: Installer?,
		heap_size: HeapSize
	): Binary {
		val scm_ref = null
		val os = getEnumFromFileName(asset.name, OperatingSystem.values())
		val architecture = getEnumFromFileName(asset.name, Architecture.values())
		val binary_type = getEnumFromFileName(asset.name, ImageType.values(), ImageType.jdk)
		val jvm_impl = getEnumFromFileName(asset.name, JvmImpl.values(), JvmImpl.hotspot)

		return Binary(
			pack,
			download_count,
			updated_at,
			scm_ref,
			installer,
			heap_size,
			os,
			architecture,
			binary_type,
			jvm_impl
		)
	}

	private fun binaryFromMetadata(
		binaryMetadata: GHMetaData, pack: Package, download_count: Long, updated_at: LocalDateTime,
		installer: Installer?, heap_size: HeapSize
	): Binary {

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
			binaryMetadata.variant
		)
	}

	private fun getChecksum(binary_checksum_link: String?): String? {
		if (binary_checksum_link != null && binary_checksum_link.isNotEmpty()) {
			LOGGER.info("Pulling checksum for ${binary_checksum_link}")

			val request = HttpRequest.newBuilder()
				.uri(URI.create(binary_checksum_link))
				.build()

			val checksum =
				HttpClientFactory.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).get()

			if (checksum.statusCode() == 200 && checksum.body() != null) {
				val tokens = checksum.body().split(" ")
				if (tokens.size > 1) {
					return tokens[0]
				}
			}
		}
		return null
	}


	private fun <T : FileNameMatcher> getEnumFromFileName(fileName: String, values: Array<T>, default: T? = null): T {

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
}