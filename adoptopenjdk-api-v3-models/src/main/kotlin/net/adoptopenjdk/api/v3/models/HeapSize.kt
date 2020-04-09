package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["normal", "large"], example = "normal")
enum class HeapSize : FileNameMatcher {
    normal, large("XL");

    override lateinit var names: List<String>

    constructor(vararg alternativeNames: String) {
        setNames(this.name, alternativeNames.toList())
    }

    override fun matchesFile(fileName: String): Boolean {
        val lowerCaseFileName = fileName.toLowerCase()
        return names
                .firstOrNull {
                    lowerCaseFileName.contains(Regex("${it}_"))
                } != null
    }
}
