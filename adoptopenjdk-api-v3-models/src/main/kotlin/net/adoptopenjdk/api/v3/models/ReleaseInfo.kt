package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

class ReleaseInfo {

    @Schema(example = "[8,9,10,11,12,13]", required = true)
    val available_releases: List<Int>

    @Schema(example = "[8,11]", required = true)
    val available_lts_releases: List<Int>

    @Schema(example = "11", required = true)
    val most_recent_lts: Int

    @Schema(example = "13", required = true)
    val most_recent_feature_release: Int

    constructor(available_releases: List<Int>, available_lts_releases: List<Int>, most_recent_lts: Int, most_recent_feature_release: Int) {
        this.available_releases = available_releases
        this.available_lts_releases = available_lts_releases
        this.most_recent_lts = most_recent_lts
        this.most_recent_feature_release = most_recent_feature_release
    }

}
