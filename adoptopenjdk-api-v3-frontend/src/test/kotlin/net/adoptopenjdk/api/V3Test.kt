package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import net.adoptopenjdk.api.v3.V3
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import kotlin.test.Test
import kotlin.test.assertNotNull

@QuarkusTest
class V3Test() : BaseTest() {

    @Test
    fun `update is scheduled`() {
        V3()
        assertNotNull(APIDataStore.schedule)
    }
}
