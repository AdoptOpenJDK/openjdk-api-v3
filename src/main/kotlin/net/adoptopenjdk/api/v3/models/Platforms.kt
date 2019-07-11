package net.adoptopenjdk.api.v3.models

import com.fasterxml.jackson.annotation.JsonProperty

class Attributes(@JsonProperty("heapSize") val heapSize: HeapSize,
                 @JsonProperty("os") val os: OperatingSystem,
                 @JsonProperty("architecture") val architecture: Architecture) {

}

class Platform(@JsonProperty("officialName") val officialName: String,
               @JsonProperty("searchableName") val searchableName: String,
               @JsonProperty("logo") val logo: String,
               @JsonProperty("attributes") val attributes: Attributes,
               @JsonProperty("binaryExtension") val binaryExtension: String,
               @JsonProperty("installerExtension") val installerExtension: String,
               @JsonProperty("installCommand") val installCommand: String,
               @JsonProperty("pathCommand") val pathCommand: String,
               @JsonProperty("checksumCommand") val checksumCommand: String,
               @JsonProperty("checksumAutoCommandHint") val checksumAutoCommandHint: String,
               @JsonProperty("checksumAutoCommand") val checksumAutoCommand: String,
               @JsonProperty("osDetectionString") val osDetectionString: String) {

}


class Platforms(@JsonProperty("platforms") val platforms: List<Platform>) {

}
