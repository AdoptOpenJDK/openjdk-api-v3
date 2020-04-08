package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema
class BinaryAssetView {

    @Schema(implementation = Binary::class)
    val binary: Binary

    @Schema(example = "jdk8u162-b12_openj9-0.8.0")
    val release_name: String

    @Schema(implementation = VersionData::class)
    val version: VersionData

    constructor(
        release_name: String,
        binary: Binary,
        version: VersionData
    ) {
        this.release_name = release_name
        this.binary = binary
        this.version = version
    }
}
