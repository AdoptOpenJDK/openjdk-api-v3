package net.adoptopenjdk.api.v3.dataSources.github

import net.adoptopenjdk.api.v3.CredentialAccessor
import net.adoptopenjdk.api.v3.SystemCredentialAccessor
import org.slf4j.LoggerFactory

class GithubAuth(private val credentialAccessor: CredentialAccessor = SystemCredentialAccessor()) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    fun readToken(): String? {
        var token = credentialAccessor.getenv("GITHUB_TOKEN")
        if (token.isNullOrEmpty()) {
            token = credentialAccessor.getProperty("GITHUB_TOKEN")
        }
        if (token.isNullOrEmpty()) {
            token = credentialAccessor.getPropertyFromFile("token", "token.properties")
        }
        if (token.isNullOrEmpty()) {
            LOGGER.error("Could not find GITHUB_TOKEN")
        }
        return token
    }
}
