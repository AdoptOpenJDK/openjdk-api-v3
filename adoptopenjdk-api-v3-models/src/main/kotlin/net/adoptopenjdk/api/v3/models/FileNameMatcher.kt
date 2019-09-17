package net.adoptopenjdk.api.v3.models

interface FileNameMatcher {
    var names: List<String>

    fun matchesFile(fileName: String): Boolean {
        val lowerCaseFileName = fileName.toLowerCase()
        return names
                .firstOrNull {
                    lowerCaseFileName.contains(Regex("[\\-_]${it}_"))
                } != null

    }

    fun setNames(instanceName: String, alternativeNames: List<String>) {
        names = listOf(instanceName)
                .union(alternativeNames.toList())
                .map { it.toLowerCase() }
                .toList()
    }
}