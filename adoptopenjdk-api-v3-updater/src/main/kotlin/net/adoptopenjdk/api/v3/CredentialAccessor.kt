package net.adoptopenjdk.api.v3

import java.nio.file.Files
import java.nio.file.Path
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
     * Searches a file ~/.adopt_api/[filename] for a property named [key].
     *
     *  For example:
     *
     *  * Given the following user system configuration:
     *      ```
     *      $ ls -d ~/.adopt_api/somefile.properties
     *      /home/foo/.adopt_api/somefile.properties
     *      $ cat /home/foo/.adopt_api/somefile.properties
     *      my.local.property=p455w3rd
     *      ```
     *
     *  * Then, the following assertion should hold
     *      ```
     *      val creds = getPropertyFromFile(
     *          key = "my.local.property",
     *          filename = "somefile.properties"
     *      )
     *      assert(creds == "p455w3rd")
     *      ```
     *
     * @param key Property name to look for
     * @param filename Name of property file located at `~/.adopt_api`
     * @return `some-value` where `~/.adopt_api`/[filename] contains the line [key]`=some-value`
     */
    override fun getPropertyFromFile(key: String, filename: String): String? {
        val userHome = System.getProperty("user.home")
        val propertiesFile = Path.of(userHome, ".adopt_api", filename).toFile()

        var token = ""
        if (propertiesFile.exists()) {
            val properties = Properties()
            properties.load(Files.newInputStream(propertiesFile.toPath()))
            token = properties.getProperty(key)
        }
        return token
    }
}
