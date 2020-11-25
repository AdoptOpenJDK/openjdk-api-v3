package net.adoptopenjdk.api.v3.dataSources.persitence.mongo

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MongoClientTest {

    @Test
    fun `default connection string`() {
        val connectionString = MongoClient.createConnectionString("api-data")
        assertEquals(connectionString, "mongodb://localhost:27017/?serverSelectionTimeoutMS=100")
    }

    @Test
    fun `default connection string with explicit server selection timeout`() {
        val connectionString = MongoClient.createConnectionString("api-data", serverSelectionTimeoutMills = "999")
        assertEquals(connectionString, "mongodb://localhost:27017/?serverSelectionTimeoutMS=999")
    }

    @Test
    fun `override with test connection string`() {
        System.setProperty("MONGODB_TEST_CONNECTION_STRING", "mongodb://some-host:99999")
        val connectionString = MongoClient.createConnectionString("api-data")
        assertEquals(connectionString, "mongodb://some-host:99999")
    }

    @Test
    fun `fully specified connection string`() {
        val connectionString = MongoClient.createConnectionString(
            "api-data",
            "some-user",
            "some-password",
            "some-host",
            "12345"
        )
        assertEquals(connectionString, "mongodb://some-user:some-password@some-host:12345/api-data")
    }
}
