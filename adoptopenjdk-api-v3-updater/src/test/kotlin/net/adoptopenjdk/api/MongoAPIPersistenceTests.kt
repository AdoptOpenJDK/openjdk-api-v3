package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import org.junit.Assert
import org.junit.jupiter.api.Test

class MongoAPIPersistenceTests : MongoTest() {
    @Test
    fun `update time is set`(apiPersistence: ApiPersistence) {
        runBlocking {
            val api = apiPersistence
            api.updateUpdatedTime(TimeSource.now(), "")
            api.updateUpdatedTime(TimeSource.now(), "")
            api.updateUpdatedTime(TimeSource.now(), "")
            val time = TimeSource.now()
            api.updateUpdatedTime(time, "")

            val stored = api.getUpdatedAt()

            Assert.assertEquals(time, stored.time)
        }
    }
}
