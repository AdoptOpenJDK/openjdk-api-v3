package net.adoptopenjdk.api.packages

import io.quarkus.test.junit.QuarkusTest
import net.adoptopenjdk.api.DbExtension
import net.adoptopenjdk.api.v3.models.*
import org.hamcrest.Matchers
import org.jboss.weld.junit5.EnableWeld
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(value = [DbExtension::class])
@QuarkusTest
@EnableWeld
class InstallerPathTest : PackageEndpointTest() {

    override fun getPath(): String {
        return "/v3/installer"
    }

    @Test
    fun latestDoesRedirectToBinary() {
        val path = getLatestPath(11, ReleaseType.ga, OperatingSystem.windows, Architecture.x64, ImageType.jdk, JvmImpl.hotspot, HeapSize.normal, Vendor.adoptopenjdk, Project.jdk)
        performRequest(path)
            .then()
            .statusCode(307)
            .header("Location", Matchers.startsWith("https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/"))
    }

    @Test
    fun latestDoesRedirectToBinaryNoProject() {
        val path = "${getPath()}/latest/11/ga/windows/x64/jdk/openj9/normal/adoptopenjdk"
        performRequest(path)
            .then()
            .statusCode(307)
            .header("Location", Matchers.startsWith("https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/"))
    }

    @Test
    fun latestDoesNotRedirectToBinary() {
        val path = getLatestPath(8, ReleaseType.ga, OperatingSystem.windows, Architecture.x64, ImageType.jdk, JvmImpl.hotspot, HeapSize.normal, Vendor.adoptopenjdk, Project.valhalla)
        performRequest(path)
            .then()
            .statusCode(404)
    }

    @Test
    fun noExistantLatestRequestGives404() {
        val path = getLatestPath(4, ReleaseType.ga, OperatingSystem.windows, Architecture.x64, ImageType.jdk, JvmImpl.hotspot, HeapSize.normal, Vendor.adoptopenjdk, Project.valhalla)
        performRequest(path)
            .then()
            .statusCode(404)
    }

    @Test
    fun versionRequestRedirects() {

        val (release, binary) = getRandomBinary()

        val path = getVersionPath(release.release_name, binary.os, binary.architecture, binary.image_type, binary.jvm_impl, binary.heap_size, release.vendor, binary.project)

        performRequest(path)
            .then()
            .statusCode(307)
            .header("Location", Matchers.equalTo(binary.installer?.link))
    }

    @Test
    fun nonExistantVersionRequestGives404() {
        val path = getVersionPath("fooBar", OperatingSystem.windows, Architecture.x64, ImageType.jdk, JvmImpl.hotspot, HeapSize.normal, Vendor.adoptopenjdk, Project.jdk)
        performRequest(path)
            .then()
            .statusCode(404)
    }
}
