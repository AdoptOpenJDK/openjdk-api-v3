package net.adoptopenjdk.api.v3.dataSources.github

import net.adoptopenjdk.api.v3.CredentialAccessor
import net.adoptopenjdk.api.v3.SystemCredentialAccessor
import org.slf4j.LoggerFactory

class GitHubAuth(private val credentialAccessor: CredentialAccessor = SystemCredentialAccessor()) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    private val tokenName = "GITHUB_TOKEN"

    fun readToken(): String? {
        var token = credentialAccessor.getenv(tokenName)
        if (token.isNullOrEmpty()) {
            token = credentialAccessor.getProperty(tokenName)
        }
        if (token.isNullOrEmpty()) {
            token = credentialAccessor.getPropertyFromFile("token", "token.properties")
        }
        if (token.isNullOrEmpty()) {
            LOGGER.error("Could not find $tokenName")
        }
        return token
    }
}
