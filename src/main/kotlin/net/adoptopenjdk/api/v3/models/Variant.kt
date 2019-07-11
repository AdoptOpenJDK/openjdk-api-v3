package net.adoptopenjdk.api.v3.models

import com.fasterxml.jackson.annotation.JsonProperty


class Variant(
        @JsonProperty("searchableName") val searchableName: String,
        @JsonProperty("jvm") val jvm: JvmImpl,
        @JsonProperty("vendor") val vendor: Vendor,
        @JsonProperty("version") val version: Int,
        @JsonProperty("lts") lts: Boolean?,
        @JsonProperty("websiteDescription") val websiteDescription: String?,
        @JsonProperty("websiteDescriptionLink") val websiteDescriptionLink: String?,
        @JsonProperty("websiteDefault") val websiteDefault: Boolean?) {

    val lts: Boolean
    val label: String
    val officialName: String

    var latest: Boolean = false

    init {
        this.lts = lts ?: false
        this.label = "${vendor} ${version}"
        this.officialName = "${vendor} ${version} with ${jvm}"
    }
}

class Variants(@JsonProperty("variants") val variants: List<Variant>) {

    val versions: List<Int>
    val latestVersion: Int

    val ltsVersions: List<Int>
    val latestLtsVersion: Int

    init {
        versions = variants.map { it.version }.sorted().distinct()
        latestVersion = versions.last()
        ltsVersions = variants.filter { it.lts }.map { it.version }.sorted().distinct()
        latestLtsVersion = ltsVersions.last()
        variants
                .forEach({
                    if (it.version == latestVersion) {
                        it.latest = true
                    }
                })
    }
}
