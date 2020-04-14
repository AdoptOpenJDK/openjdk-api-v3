package net.adoptopenjdk.api

import net.adoptopenjdk.api.v3.dataSources.github.GithubAuth
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

class GitHubAuthTest {
    @Test
    fun readsTokenFromFile() {
        System.setProperty("GITHUB_TOKEN", null)
        System.setProperty("user.home", "/tmp")
        Files.createDirectory(Path.of("/tmp/.adopt_api"))
        File("/tmp/.adopt_api/token.properties").printWriter().use { out ->
            out.println("token=foo")
        }
        try {
            assertEquals("foo", GithubAuth.readToken())
        } finally {
            Files.delete(Path.of("/tmp/.adopt_api/token.properties"))
            Files.delete(Path.of("/tmp/.adopt_api"))
        }
    }

    @Test
    fun readsTokenNullFromFile() {
        System.setProperty("user.home", "/tmp")
        assertEquals(null, GithubAuth.readToken())
    }
}
