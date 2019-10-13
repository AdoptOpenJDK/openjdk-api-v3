package net.adoptopenjdk.api.v3.models

import com.fasterxml.jackson.annotation.JsonProperty

class Variant {

    val searchableName: String
    val jvm: JvmImpl
    val vendor: Vendor
    val version: Int
    val lts: Boolean
    val websiteDescription: String?
    val websiteDescriptionLink: String?
    val websiteDefault: Boolean?
    val label: String
    val officialName: String

    var latest: Boolean = false

    constructor(
            @JsonProperty("searchableName")
            searchableName: String,
            @JsonProperty("jvm")
            jvm: JvmImpl,
            @JsonProperty("vendor")
            vendor: Vendor,
            @JsonProperty("version")
            version: Int,
            @JsonProperty("lts")
            lts: Boolean?,
            @JsonProperty("websiteDescription")
            websiteDescription: String?,
            @JsonProperty("websiteDescriptionLink")
            websiteDescriptionLink: String?,
            @JsonProperty("websiteDefault")
            websiteDefault: Boolean?) {
        this.searchableName = searchableName
        this.jvm = jvm
        this.vendor = vendor
        this.version = version
        this.lts = lts ?: false
        this.websiteDescription = websiteDescription
        this.websiteDescriptionLink = websiteDescriptionLink
        this.websiteDefault = websiteDefault
        this.label = "$vendor $version"
        this.officialName = "$vendor $version with $jvm"

    }
}

class Variants {

    val variants: Array<Variant>
    val versions: Array<Int>
    val latestVersion: Int
    val ltsVersions: Array<Int>
    val latestLtsVersion: Int

    constructor(
            @JsonProperty("variants")
            variants: Array<Variant>) {
        this.variants = variants

        versions = variants.map { it.version }.sorted().distinct().toTypedArray()
        latestVersion = versions.last()

        ltsVersions = variants.filter { it.lts }.map { it.version }.sorted().distinct().toTypedArray()
        latestLtsVersion = ltsVersions.last()

        variants
                .forEach {
                    if (it.version == latestVersion) {
                        it.latest = true
                    }
                }
    }
}
