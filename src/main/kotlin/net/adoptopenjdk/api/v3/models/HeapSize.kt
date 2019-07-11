package net.adoptopenjdk.api.v3.models;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["normal", "large"], example = "normal")
enum class HeapSize {
    normal, large;
}