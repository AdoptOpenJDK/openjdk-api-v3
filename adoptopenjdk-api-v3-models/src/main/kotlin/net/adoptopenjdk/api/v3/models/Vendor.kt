package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, defaultValue = "adoptopenjdk", enumeration = ["adoptopenjdk", "ibm", "redhat", "azul", "amazon"], example = "adoptopenjdk")
enum class Vendor {
    adoptopenjdk, ibm, redhat, azul, amazon;
}
