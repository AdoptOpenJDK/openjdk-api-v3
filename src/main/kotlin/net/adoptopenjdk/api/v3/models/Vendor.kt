package net.adoptopenjdk.api.v3.models;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, defaultValue = "adopt", enumeration = ["adopt", "ibm", "redhat", "azul", "amazon"], example = "adopt")
enum class Vendor {
    adopt, ibm, redhat, azul, amazon;
}