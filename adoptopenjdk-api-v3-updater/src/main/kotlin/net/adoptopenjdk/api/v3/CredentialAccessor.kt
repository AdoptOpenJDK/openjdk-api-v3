package net.adoptopenjdk.api.v3

import java.io.File
import java.nio.file.Files
import java.util.Properties

interface CredentialAccessor {
    fun getenv(key: String): String?
    fun getProperty(key: String): String?
    fun getPropertyFromFile(key: String, filename: String): String?
}

class SystemCredentialAccessor : CredentialAccessor {
    override fun getenv(key: String): String? = System.getenv(key) ?: null
    override fun getProperty(key: String): String? = System.getProperty(key) ?: null

    /**
     * Searches a file ~/.adopt_api/<filename> for a property named <key>
     */
    override fun getPropertyFromFile(key: String, filename: String): String? {
        val userHome = System.getProperty("user.home")

        // e.g /home/foo/.adopt_api/token.properties
        val propertiesFile = File(userHome + File.separator + ".adopt_api" + File.separator + filename)

        var token = ""
        if (propertiesFile.exists()) {
            val properties = Properties()
            properties.load(Files.newInputStream(propertiesFile.toPath()))
            token = properties.getProperty(key)
        }
        return token
    }
}
