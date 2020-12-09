package net.adoptopenjdk.api

import net.adoptopenjdk.api.v3.TimeSource
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepo
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.Binary
import net.adoptopenjdk.api.v3.models.HeapSize
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.Installer
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.Package
import net.adoptopenjdk.api.v3.models.Project
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.SourcePackage
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import java.time.ZonedDateTime
import java.util.Random

object AdoptReposTestDataGenerator {

    var rand: Random = Random(1)
    val TEST_VERSIONS = listOf(8, 9, 10, 11, 12)
    private val TEST_RESOURCES = listOf(
        PermittedValues(
            ReleaseType.values().asList(),
            listOf(Vendor.adoptopenjdk),
            listOf(Project.jdk, Project.jfr),
            listOf(JvmImpl.hotspot),
            ImageType.values().asList(),
            Architecture.values().asList(),
            OperatingSystem.values().asList(),
            listOf(HeapSize.normal)
        ),
        PermittedValues(
            ReleaseType.values().asList(),
            listOf(Vendor.adoptopenjdk),
            listOf(Project.jdk),
            listOf(JvmImpl.openj9),
            ImageType.values().asList(),
            Architecture.values().asList(),
            OperatingSystem.values().asList(),
            HeapSize.values().asList()
        ),
        PermittedValues(
            ReleaseType.values().asList(),
            listOf(Vendor.openjdk),
            listOf(Project.jdk),
            listOf(JvmImpl.hotspot),
            ImageType.values().asList(),
            Architecture.values().asList(),
            OperatingSystem.values().asList(),
            listOf(HeapSize.normal)
        )
    )

    fun generate(): AdoptRepos {
        rand = Random(1)
        return AdoptRepos(
            TEST_VERSIONS.associateWith { version ->
                FeatureRelease(version, createRepos(version))
            }
        )
    }

    private fun createRepos(majorVersion: Int): List<AdoptRepo> {
        return (1..2)
            .flatMap {
                TEST_RESOURCES.map { AdoptRepo(it.createReleases(majorVersion)) }
            }
            .toList()
    }

    class PermittedValues(
        val releaseType: List<ReleaseType>,
        val vendor: List<Vendor>,
        val project: List<Project>,
        val jvmImpl: List<JvmImpl>,
        val imageType: List<ImageType>,
        val architecture: List<Architecture>,
        val operatingSystem: List<OperatingSystem>,
        val heapSize: List<HeapSize>
    ) {
        private fun releaseBuilder(): (ReleaseType) -> (Vendor) -> (VersionData) -> Release {
            return { releaseType: ReleaseType ->
                { vendor: Vendor ->
                    { versionData: VersionData ->
                        Release(
                            randomString("release id"),
                            releaseType,
                            randomString("release lin"),
                            randomString("release name"),
                            randomDate(),
                            randomDate(),
                            getBinaries(),
                            2,
                            vendor,
                            versionData,
                            sourcePackage()
                        )
                    }
                }
            }
        }

        private fun createPackage(): Package {
            return Package(
                randomString("package name"),
                "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/",
                rand.nextLong(),
                randomString("checksum"),
                randomString("checksum link"),
                1,
                randomString("signature link"),
                randomString("metadata link")
            )
        }

        private fun createInstaller(): Installer {
            return Installer(
                randomString("installer name"),
                "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/",
                2,
                randomString("checksum"),
                randomString("checksum link"),
                3,
                randomString("signature link"),
                randomString("metadata link")
            )
        }

        private fun binaryBuilder(): (HeapSize) -> (OperatingSystem) -> (Architecture) -> (ImageType) -> (JvmImpl) -> (Project) -> Binary {
            return { heapSize ->
                { operatingSystem ->
                    { architecture ->
                        { imageType ->
                            { jvmImpl ->
                                { project ->
                                    Binary(
                                        createPackage(),
                                        1,
                                        randomDate(),
                                        randomString("scm ref"),
                                        createInstaller(),
                                        heapSize,
                                        operatingSystem,
                                        architecture,
                                        imageType,
                                        jvmImpl,
                                        project
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        fun createReleases(majorVersion: Int): List<Release> {
            return releaseType
                .map { releaseBuilder()(it) }
                .flatMap { builder -> vendor.map { builder(it) } }
                .flatMap { builder -> getVersions(majorVersion).map { builder(it) } }
        }

        private fun getBinaries(): Array<Binary> {
            return heapSize.map { binaryBuilder()(it) }
                .flatMap { builder -> operatingSystem.map { builder(it) } }
                .flatMap { builder -> architecture.map { builder(it) } }
                .flatMap { builder -> imageType.map { builder(it) } }
                .flatMap { builder -> jvmImpl.map { builder(it) } }
                .flatMap { builder -> project.map { builder(it) } }
                .toTypedArray()
        }
    }

    private fun sourcePackage(): SourcePackage? {
        return SourcePackage(randomString("source package name"), randomString("source package link"), rand.nextLong())
    }

    fun getVersions(majorVersion: Int): List<VersionData> {
        return listOf(
            VersionData(majorVersion, 0, 200, null, 1, 2, null, ""),
            VersionData(majorVersion, 0, 201, null, 1, 2, null, "")
        )
    }

    private fun randomDate(): ZonedDateTime {
        return TimeSource.now().minusDays(10)
    }

    fun randomString(prefix: String): String {
        return prefix + ": " + rand.nextInt().toString()
    }
}
