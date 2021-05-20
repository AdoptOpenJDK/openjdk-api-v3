package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

/*
    Use a schema ref as adopt and adoptium will have a different subset of implementations
*/
@Schema(ref = JvmImplSchemaRef.SCHEMA_REF)
enum class JvmImpl : FileNameMatcher {
    hotspot, openj9;

    override lateinit var names: List<String>
    override var priority: Int = 0

    constructor(priority: Int = 0, vararg alternativeNames: String) {
        this.priority = priority
        setNames(this.name, alternativeNames.toList())
    }
}
