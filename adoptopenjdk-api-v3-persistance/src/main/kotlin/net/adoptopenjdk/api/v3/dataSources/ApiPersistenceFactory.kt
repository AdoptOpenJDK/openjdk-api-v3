package net.adoptopenjdk.api.v3.dataSources

import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoApiPersistence


object ApiPersistenceFactory {
    // Current default impl is mongo impl
    private var impl: ApiPersistence? = null


    fun get(): ApiPersistence {
        if (impl == null) {
            impl = MongoApiPersistence()
        }
        return impl!!
    }

    fun set(impl: ApiPersistence?) {
        this.impl = impl
    }

}