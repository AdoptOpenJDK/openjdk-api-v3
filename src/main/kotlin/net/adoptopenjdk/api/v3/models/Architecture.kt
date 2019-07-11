package net.adoptopenjdk.api.v3.models;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["x64", "x32", "ppc64", "ppc64le", "s390x", "aarch64", "arm", "sparcv9"], example = "x64")
enum class Architecture {
    x64, x32, ppc64, ppc64le, s390x, aarch64, arm, sparcv9;
}