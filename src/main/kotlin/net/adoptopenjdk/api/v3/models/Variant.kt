package net.adoptopenjdk.api.v3.models

class Variant(
        val searchableName: String,
        val jvm: JvmImpl,
        val vendor: Vendor,
        val version: Int,
        lts: Boolean?,
        val websiteDescription: String?,
        val websiteDescriptionLink: String?,
        val websiteDefault: Boolean?) {

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

class Variants(val variants: List<Variant>) {

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
