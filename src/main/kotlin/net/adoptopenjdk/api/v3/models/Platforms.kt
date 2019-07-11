package net.adoptopenjdk.api.v3.models

class Attributes(val heapSize: HeapSize,
                 val os: OperatingSystem,
                 val architecture: Architecture)

class Platform(val officialName: String,
               val searchableName: String,
               val logo: String,
               val attributes: Attributes,
               val binaryExtension: String,
               val installerExtension: String,
               val installCommand: String,
               val pathCommand: String,
               val checksumCommand: String,
               val checksumAutoCommandHint: String,
               val checksumAutoCommand: String,
               val osDetectionString: String)

class Platforms(val platforms: List<Platform>)
