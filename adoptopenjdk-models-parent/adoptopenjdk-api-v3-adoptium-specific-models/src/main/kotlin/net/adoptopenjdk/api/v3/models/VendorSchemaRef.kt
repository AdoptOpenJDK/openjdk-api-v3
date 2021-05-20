package net.adoptopenjdk.api.v3.models

object VendorSchemaRef {
    fun getDefault(): String {
        return AdoptiumVendor.DEFAULT_VALUE
    }

    const val SCHEMA_REF = "#/components/schemas/AdoptiumVendor"
}
