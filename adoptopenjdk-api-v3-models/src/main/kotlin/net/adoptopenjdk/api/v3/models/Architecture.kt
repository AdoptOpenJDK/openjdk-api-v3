package net.adoptopenjdk.api.v3.models

import com.fasterxml.jackson.annotation.JsonCreator
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["x64", "x32", "ppc64", "ppc64le", "s390x", "aarch64", "arm", "sparcv9", "riscv64"])
enum class Architecture : FileNameMatcher {
    x64,
    x32("x86-32"),
    ppc64,
    ppc64le,
    s390x,
    aarch64,
    arm("arm32"),
    sparcv9,
    riscv64
    ;

    override lateinit var names: List<String>

    constructor(vararg alternativeNames: String) {
        setNames(this.name, alternativeNames.toList())
    }

    companion object {

        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Architecture {
            return values()
                .filter { it.names.contains(value) }
                .first()
        }
    }
}
