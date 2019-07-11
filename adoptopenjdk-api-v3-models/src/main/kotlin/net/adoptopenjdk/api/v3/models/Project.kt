package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, defaultValue = "jdk", enumeration = ["jdk", "valhalla", "metropolis"], example = "jdk")
enum class Project {
    jdk, valhalla, metropolis;
}
