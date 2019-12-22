package net.adoptopenjdk.api

import io.vertx.core.json.JsonObject
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHMetaData
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHVersion
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.OperatingSystem
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class MetadataSerializationTest {

    fun generateMetadata(): GHMetaData {
        return GHMetaData("a",
                OperatingSystem.aix,
                Architecture.aarch64,
                "hotspot",
                GHVersion(

                        1, 2, 3,
                        "a",
                        4,
                        "b",
                        5,
                        "c",
                        "d"
                ),
                "b",
                "c",
                ImageType.jdk,
                "d"
        )
    }

    @Test
    fun canAddNewUnknownFieldsToMetadata() {

        val metadata = generateMetadata()

        val serialized = JsonMapper.mapper.writeValueAsString(metadata);
        var json = JsonObject(serialized)
        json = json.put("version.foobar", "foo");

        val reparsed = JsonMapper.mapper.readValue(json.encode(), GHMetaData::class.java)

        assertEquals(metadata, reparsed)
    }


    @Test
    fun canRemoveWarningFieldsToMetadata() {

        val metadata = generateMetadata()

        val serialized = JsonMapper.mapper.writeValueAsString(metadata);
        val json = JsonObject(serialized)
        json.remove("WARNING");

        val noWarning = GHMetaData(null, metadata.os, metadata.arch, metadata.variant, metadata.version, metadata.scmRef, metadata.version_data, metadata.binary_type, metadata.sha256);

        val reparsed = JsonMapper.mapper.readValue(json.encode(), GHMetaData::class.java)

        assertEquals(noWarning, reparsed)
    }
}