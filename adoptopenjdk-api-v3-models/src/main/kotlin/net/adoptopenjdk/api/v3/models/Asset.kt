package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

open class Asset {

    @Schema(example = "OpenJDK8U-jre_x86-32_windows_hotspot_8u212b04.msi")
    val name: String

    @Schema(example = "https://github.com/AdoptOpenJDK/openjdk8-binaries/ga/download/jdk8u212-b04/OpenJDK8U-jre_x86-32_windows_hotspot_8u212b04.msi")
    val link: String

    @Schema(example = "82573385")
    val size: Long

    @Schema(example = "dd28d6d2cde2b931caf94ac2422a2ad082ea62f0beee3bf7057317c53093de93")
    val checksum: String?

    @Schema(example = "https://github.com/AdoptOpenJDK/openjdk8-openj9-releases/ga/download/jdk8u162-b12_openj9-0.8.0/OpenJDK8-OPENJ9_x64_Linux_jdk8u162-b12_openj9-0.8.0.tar.gz.sha256.txt")
    val checksum_link: String?

    constructor(
            name: String,
            link: String,
            size: Long,
            checksum: String?,
            checksum_link: String?) {
        this.name = name
        this.link = link
        this.size = size
        this.checksum = checksum
        this.checksum_link = checksum_link
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Asset

        if (name != other.name) return false
        if (link != other.link) return false
        if (size != other.size) return false
        if (checksum != other.checksum) return false
        if (checksum_link != other.checksum_link) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + link.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + (checksum?.hashCode() ?: 0)
        result = 31 * result + (checksum_link?.hashCode() ?: 0)
        return result
    }
}
