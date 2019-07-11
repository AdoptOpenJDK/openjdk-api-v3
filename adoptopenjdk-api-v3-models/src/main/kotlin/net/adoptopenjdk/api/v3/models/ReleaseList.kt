package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

class ReleaseList {

    @Schema(required = true)
    val releases: List<String>

    constructor(releases: List<String>) {
        this.releases = releases
    }

}
