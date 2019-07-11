package net.adoptopenjdk.api.v3.models;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, defaultValue = "releases", enumeration = ["releases", "nightly"], example = "class")
enum class ReleaseType {
    releases,
    nightly;
}