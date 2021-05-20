package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.media.Schema

/*
    Use a schema ref as adopt and adoptium will have a different subset of vendors
*/
@Schema(ref = VendorSchemaRef.SCHEMA_REF)
enum class Vendor {
    adoptium, adoptopenjdk, openjdk;
    // , amazon, azul, bellsoft, , sap;

    companion object {
        fun getDefault(): Vendor {
            return valueOf(VendorSchemaRef.getDefault())
        }
    }
}
