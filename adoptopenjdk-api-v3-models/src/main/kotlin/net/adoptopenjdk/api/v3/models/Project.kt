package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(
    type = SchemaType.STRING,
    name = "project",
    description = "Project",
    defaultValue = "jdk",
    enumeration = ["jdk", "valhalla", "metropolis", "jfr"],
    required = false
)
enum class Project : FileNameMatcher {
    jdk,
    valhalla,
    metropolis,
    jfr("-jfr_");

    override lateinit var names: List<String>

    constructor(vararg alternativeNames: String) {
        setNames(this.name, alternativeNames.toList())
    }
}
