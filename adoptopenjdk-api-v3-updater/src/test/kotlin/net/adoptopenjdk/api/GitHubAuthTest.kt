package net.adoptopenjdk.api

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import net.adoptopenjdk.api.v3.CredentialAccessor
import net.adoptopenjdk.api.v3.dataSources.github.GithubAuth
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.condition.DisabledIfSystemProperty
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class GitHubAuthTest {

    @MockK
    private lateinit var mockCredentialAccessor: CredentialAccessor

    companion object {
        const val tokenKey = "GITHUB_TOKEN"
    }

    @Test
    fun `readToken prioritizes env var if it is defined`() {
        every { mockCredentialAccessor.getenv(any()) } returns "env-token"

        val actualToken = GithubAuth(mockCredentialAccessor).readToken()

        assertEquals("env-token", actualToken)

        verify(exactly = 1) { mockCredentialAccessor.getenv(tokenKey) }
        confirmVerified(mockCredentialAccessor)
    }

    @Test
    fun `readToken prioritizes property if env var is not defined`() {
        every { mockCredentialAccessor.getenv(any()) } returns null
        every { mockCredentialAccessor.getProperty(any()) } returns "property-token"

        val actualToken = GithubAuth(mockCredentialAccessor).readToken()

        assertEquals("property-token", actualToken)

        verify(exactly = 1) { mockCredentialAccessor.getenv(tokenKey) }
        verify(exactly = 1) { mockCredentialAccessor.getProperty(tokenKey) }
        confirmVerified(mockCredentialAccessor)
    }

    @Test
    fun `readToken falls back to reading file if env var and property are not defined`() {
        every { mockCredentialAccessor.getenv(any()) } returns null
        every { mockCredentialAccessor.getProperty(any()) } returns null
        every { mockCredentialAccessor.getPropertyFromFile(any(), any()) } returns "file-token"

        val actualToken = GithubAuth(mockCredentialAccessor).readToken()

        assertEquals("file-token", actualToken)

        verify(exactly = 1) { mockCredentialAccessor.getenv(tokenKey) }
        verify(exactly = 1) { mockCredentialAccessor.getProperty(tokenKey) }
        verify(exactly = 1) { mockCredentialAccessor.getPropertyFromFile("token", "token.properties") }
        confirmVerified(mockCredentialAccessor)
    }

    /**
     * SystemCredentialAccessor makes actual calls to System.getenv and System.getProperty, so ignore these tests if an
     * env var or property named GITHUB_TOKEN exists to prevent real tokens being inadvertently logged as assertion errors.
     */
    @Nested
    @DisabledIfEnvironmentVariable(named = tokenKey, matches = ".*")
    @DisabledIfSystemProperty(named = tokenKey, matches = ".*")
    inner class SystemCredentialAccessorTest {

        private lateinit var tempDir: File
        private lateinit var tokenFile: File
        private lateinit var savedUserHome: String

        @BeforeEach
        fun setUp() {
            assertTrue(System.getenv(tokenKey) == null)
            assertTrue(System.getProperty(tokenKey) == null)

            tempDir = Files.createTempDirectory("testUserHome").toFile()

            savedUserHome = System.getProperty("user.home")
            System.setProperty("user.home", tempDir.absolutePath)
        }

        @AfterEach
        fun cleanUp() {
            tempDir.deleteRecursively()
            System.setProperty("user.home", savedUserHome)
        }

        @Test
        fun `GithubAuth uses SystemCredentialAccessor by default`() {
            val tokenDir = File(tempDir, ".adopt_api")
            tokenDir.mkdirs()
            tokenFile = File(tokenDir, "token.properties")
            tokenFile.printWriter().use { out ->
                out.println("token=real-file-token")
            }

            val actualToken = GithubAuth().readToken()

            assertEquals("real-file-token", actualToken)
        }

        @Test
        fun readsTokenNullFromFile() {
            assertFalse(File(tempDir, ".adopt_api").exists())

            assertEquals("", GithubAuth().readToken())
        }
    }
}
