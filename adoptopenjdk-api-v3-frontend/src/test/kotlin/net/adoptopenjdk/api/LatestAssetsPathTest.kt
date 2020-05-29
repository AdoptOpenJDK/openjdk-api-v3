package net.adoptopenjdk.api

/* ktlint-disable no-wildcard-imports */
/* ktlint-enable no-wildcard-imports */
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.vertx.core.json.JsonArray
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.BinaryAssetView
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import org.junit.jupiter.api.Test

@QuarkusTest
class LatestAssetsPathTest : FrontEndTest() {

    fun getPath() = "/v3/assets/latest"

    @Test
    fun latestAssetsReturnsSaneList() {

        val body = RestAssured.given()
            .`when`()
            .get("${getPath()}/8/${JvmImpl.hotspot}")
            .body

        val binaryStr = body.prettyPrint()

        val binaries = JsonArray(binaryStr)

        assert(hasEntryFor(binaries, OperatingSystem.linux, ImageType.jdk, Architecture.x64))
        assert(hasEntryFor(binaries, OperatingSystem.linux, ImageType.jre, Architecture.x64))
        assert(hasEntryFor(binaries, OperatingSystem.windows, ImageType.jdk, Architecture.x64))
        assert(hasEntryFor(binaries, OperatingSystem.windows, ImageType.jdk, Architecture.x64))
    }

    private fun hasEntryFor(binaries: JsonArray, os: OperatingSystem, imageType: ImageType, architecture: Architecture): Boolean {
        val hasEntry = binaries
            .map { JsonMapper.mapper.readValue(it.toString(), BinaryAssetView::class.java) }
            .filter({ release ->
                release.binary.os == os &&
                    release.binary.image_type == imageType &&
                    release.binary.architecture == architecture
            })
            .count() > 0
        return hasEntry
    }
}
