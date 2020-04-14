package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["linux", "windows", "mac", "solaris", "aix"])
enum class OperatingSystem : FileNameMatcher {
    linux("LinuxLH"),
    windows("win"),
    mac,
    solaris,
    aix;

    override lateinit var names: List<String>

    constructor(vararg alternativeNames: String) {
        setNames(this.name, alternativeNames.toList())
    }
}
