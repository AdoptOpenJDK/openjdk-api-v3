package net.adoptopenjdk.api.v3.models;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["linux", "windows", "mac", "solaris", "aix"], example = "linux")
enum class OperatingSystem {
    linux, windows, mac, solaris, aix;
}
