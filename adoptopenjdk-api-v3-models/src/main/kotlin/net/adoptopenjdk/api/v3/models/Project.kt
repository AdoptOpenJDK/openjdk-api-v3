package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

// If you are adding to this class, due to limitiations on annotations the enumeration arg on this annotation is duplicated
// in a number of locations, unfortunately you must go update them all
@Schema(type = SchemaType.STRING, defaultValue = "jdk", enumeration = ["jdk", "valhalla", "metropolis", "jfr"], example = "jdk")
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