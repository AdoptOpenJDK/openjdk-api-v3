package net.adoptopenjdk.api.v3.models

import com.fasterxml.jackson.annotation.JsonProperty
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

class Attributes {
    val heapSize: HeapSize
    val os: OperatingSystem
    val architecture: Architecture

    constructor(
        @JsonProperty("heapSize")
        heapSize: HeapSize,

        @JsonProperty("os")
        os: OperatingSystem,

        @JsonProperty("architecture")
        architecture: Architecture
    ) {
        this.heapSize = heapSize
        this.os = os
        this.architecture = architecture
    }
}

class Platform {

    val officialName: String
    val searchableName: String
    val logo: String
    val attributes: Attributes
    val binaryExtension: String
    val installerExtension: String
    val installCommand: String
    val pathCommand: String
    val checksumCommand: String
    val checksumAutoCommandHint: String
    val checksumAutoCommand: String
    val osDetectionString: String

    constructor(
        @JsonProperty("officialName")
        officialName: String,
        @JsonProperty("searchableName")
        searchableName: String,
        @JsonProperty("logo")
        logo: String,
        @JsonProperty("attributes")
        attributes: Attributes,
        @JsonProperty("binaryExtension")
        binaryExtension: String,
        @JsonProperty("installerExtension")
        installerExtension: String,
        @JsonProperty("installCommand")
        installCommand: String,
        @JsonProperty("pathCommand")
        pathCommand: String,
        @JsonProperty("checksumCommand")
        checksumCommand: String,
        @JsonProperty("checksumAutoCommandHint")
        checksumAutoCommandHint: String,
        @JsonProperty("checksumAutoCommand")
        checksumAutoCommand: String,
        @JsonProperty("osDetectionString")
        osDetectionString: String
    ) {
        this.officialName = officialName
        this.searchableName = searchableName
        this.logo = logo
        this.attributes = attributes
        this.binaryExtension = binaryExtension
        this.installerExtension = installerExtension
        this.installCommand = installCommand
        this.pathCommand = pathCommand
        this.checksumCommand = checksumCommand
        this.checksumAutoCommandHint = checksumAutoCommandHint
        this.checksumAutoCommand = checksumAutoCommand
        this.osDetectionString = osDetectionString
    }
}

class Platforms {
    @Schema(type = SchemaType.ARRAY, implementation = Platform::class)
    val platforms: Array<Platform>

    constructor(
        @JsonProperty("platforms")
        platforms: Array<Platform>
    ) {
        this.platforms = platforms
    }
}
