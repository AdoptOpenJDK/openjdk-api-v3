package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

class ReleaseInfo {

    @Schema(example = "[8,9,10,11,12,13,14]")
    val available_releases: Array<Int>

    @Schema(example = "[8,11]")
    val available_lts_releases: Array<Int>

    @Schema(example = "11")
    val most_recent_lts: Int

    @Schema(example = "13")
    val most_recent_feature_release: Int

    constructor(available_releases: Array<Int>, available_lts_releases: Array<Int>, most_recent_lts: Int, most_recent_feature_release: Int) {
        this.available_releases = available_releases
        this.available_lts_releases = available_lts_releases
        this.most_recent_lts = most_recent_lts
        this.most_recent_feature_release = most_recent_feature_release
    }

}
