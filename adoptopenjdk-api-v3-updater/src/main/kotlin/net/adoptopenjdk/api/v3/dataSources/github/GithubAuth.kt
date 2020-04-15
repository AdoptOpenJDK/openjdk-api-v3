package net.adoptopenjdk.api.v3.dataSources.github

import java.io.File
import java.nio.file.Files
/* ktlint-disable no-wildcard-imports */
import java.util.*
/* ktlint-enable no-wildcard-imports */
import org.slf4j.LoggerFactory

class GithubAuth {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        fun readToken(): String? {
            var token = System.getenv("GITHUB_TOKEN")
            if (token.isNullOrEmpty()) {
                token = System.getProperty("GITHUB_TOKEN")
            }

            if (token.isNullOrEmpty()) {

                val userHome = System.getProperty("user.home")

                // e.g /home/foo/.adopt_api/token.properties
                val propertiesFile = File(userHome + File.separator + ".adopt_api" + File.separator + "token.properties")

                if (propertiesFile.exists()) {

                    val properties = Properties()
                    properties.load(Files.newInputStream(propertiesFile.toPath()))
                    token = properties.getProperty("token")
                }
            }
            if (token.isNullOrEmpty()) {
                LOGGER.error("Could not find GITHUB_TOKEN")
            }
            return token
        }
    }
}
