package net.adoptopenjdk.api.v3.models;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["hotspot", "openj9"], example = "hotspot")
enum class JvmImpl {
    hotspot, openj9;
}