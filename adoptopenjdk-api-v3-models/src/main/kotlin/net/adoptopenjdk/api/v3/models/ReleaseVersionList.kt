package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

class ReleaseVersionList {

    @Schema(required = true)
    val versions: List<VersionData>

    constructor(versions: List<VersionData>) {
        this.versions = versions
    }

}
