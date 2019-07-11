package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.LocalDateTime

@Schema
class Binary {

    val os: OperatingSystem

    @Schema(example = "x64", required = true)
    val architecture: Architecture

    @Schema(example = "jdk", required = true)
    val image_type: ImageType

    @Schema(example = "openj9", required = true)
    val jvm_impl: JvmImpl

    @Schema(required = false, implementation = Package::class)
    val `package`: Package?

    @Schema(required = false, implementation = Installer::class)
    val installer: Installer?

    @Schema(example = "normal", required = true)
    val heap_size: HeapSize

    @Schema(example = "3899", required = true)
    val download_count: Long

    @Schema(example = "2018-03-15T12:13:07.000Z", required = true)
    val updated_at: LocalDateTime

    @Schema(example = "dd28d6d2cde2b931caf94ac2422a2ad082ea62f0beee3bf7057317c53093de93")
    val scm_ref: String?

    constructor(
            `package`: Package?,
            download_count: Long,
            updated_at: LocalDateTime,
            scm_ref: String?,
            installer: Installer?,
            heap_size: HeapSize,
            os: OperatingSystem,
            architecture: Architecture,
            image_type: ImageType,
            jvm_impl: JvmImpl) {
        this.`package` = `package`
        this.download_count = download_count
        this.updated_at = updated_at
        this.scm_ref = scm_ref
        this.installer = installer
        this.heap_size = heap_size
        this.os = os
        this.architecture = architecture
        this.image_type = image_type
        this.jvm_impl = jvm_impl
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Binary

        if (os != other.os) return false
        if (architecture != other.architecture) return false
        if (image_type != other.image_type) return false
        if (jvm_impl != other.jvm_impl) return false
        if (`package` != other.`package`) return false
        if (installer != other.installer) return false
        if (heap_size != other.heap_size) return false
        if (download_count != other.download_count) return false
        if (updated_at != other.updated_at) return false
        if (scm_ref != other.scm_ref) return false

        return true
    }

    override fun hashCode(): Int {
        var result = os.hashCode()
        result = 31 * result + architecture.hashCode()
        result = 31 * result + image_type.hashCode()
        result = 31 * result + jvm_impl.hashCode()
        result = 31 * result + (`package`?.hashCode() ?: 0)
        result = 31 * result + (installer?.hashCode() ?: 0)
        result = 31 * result + heap_size.hashCode()
        result = 31 * result + download_count.hashCode()
        result = 31 * result + updated_at.hashCode()
        result = 31 * result + (scm_ref?.hashCode() ?: 0)
        return result
    }

}
