package net.adoptopenjdk.api.v3.models

object VendorSchemaRef {
    fun getDefault(): String {
        return AdoptVendor.DEFAULT_VALUE
    }

    const val SCHEMA_REF = "#/components/schemas/AdoptVendor"
}
