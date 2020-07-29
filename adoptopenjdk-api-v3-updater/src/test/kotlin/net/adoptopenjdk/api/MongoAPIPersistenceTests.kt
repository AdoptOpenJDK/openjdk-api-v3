package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import org.junit.Assert
import org.junit.jupiter.api.Test

class MongoAPIPersistenceTests : BaseTest() {

    @Test
    fun `update time is set`() {
        runBlocking {
            val api = ApiPersistenceFactory.get()
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
