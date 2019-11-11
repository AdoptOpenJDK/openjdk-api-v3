package net.adoptopenjdk.api.v3.dataSources

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema


@Schema(type = SchemaType.STRING, enumeration = ["ASC", "DES"], example = "DES")
enum class SortOrder {
    ASC,
    DES;
}