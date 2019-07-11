package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

class VersionData {


    val minor: Int
    val security: Int
    val pre: String
    val adopt_build_number: Int
    val major: Int

    @Schema(example = "11.0.0+28")
    val semver: String

    @Schema(example = "11.0.4+10-201907081820")
    val openjdk_version: String
    val build: Int
    val optional: String

    constructor(minor: Int, security: Int, pre: String, adopt_build_number: Int, major: Int, semver: String, build: Int, optional: String, openjdk_version: String) {
        this.minor = minor
        this.security = security
        this.pre = pre
        this.adopt_build_number = adopt_build_number
        this.major = major
        this.semver = semver
        this.build = build
        this.optional = optional
        this.openjdk_version = openjdk_version
    }
}
