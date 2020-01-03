package net.adoptopenjdk.api.v3.dataSources.github

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.system.exitProcess

class GithubAuth {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        fun readToken(): String {
            var token = System.getenv("GITHUB_TOKEN")
            if (token == null) {
                token = System.getProperty("GITHUB_TOKEN")
            }

            if (token == null) {

                val userHome = System.getProperty("user.home")

                // e.g /home/foo/.adopt_api/token.properties
                val propertiesFile = File(userHome + File.separator + ".adopt_api" + File.separator + "token.properties")

                if (propertiesFile.exists()) {

                    val properties = Properties()
                    properties.load(Files.newInputStream(propertiesFile.toPath()))
                    token = properties.getProperty("token")
                }

            }
            if (token == null) {
                LOGGER.error("Could not find GITHUB_TOKEN")
                exitProcess(1)
            }
            return token
        }
    }

}