package dev.franz.cli.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@DisplayName("CredentialResolver")
class CredentialResolverTest {
    
    private lateinit var resolver: CredentialResolver
    
    @TempDir
    lateinit var tempDir: Path
    
    @BeforeEach
    fun setUp() {
        resolver = CredentialResolver()
    }
    
    @Nested
    @DisplayName("resolve()")
    inner class ResolveTest {
        
        @Test
        fun `returns inline value when no special prefix`() {
            val result = resolver.resolve("plain-password")
            
            assertThat(result).isEqualTo("plain-password")
        }
        
        @Test
        fun `returns null for null input`() {
            val result = resolver.resolve(null)
            
            assertThat(result).isNull()
        }
        
        @Test
        fun `returns empty string for empty input`() {
            val result = resolver.resolve("")
            
            assertThat(result).isEqualTo("")
        }
    }
    
    @Nested
    @DisplayName("resolveFile()")
    inner class ResolveFileTest {
        
        @Test
        fun `reads password from file`() {
            val passwordFile = tempDir.resolve("password.txt").toFile()
            passwordFile.writeText("secret-from-file")
            
            val result = resolver.resolveFile(passwordFile.absolutePath)
            
            assertThat(result).isEqualTo("secret-from-file")
        }
        
        @Test
        fun `trims whitespace from file content`() {
            val passwordFile = tempDir.resolve("password.txt").toFile()
            passwordFile.writeText("  secret-with-spaces  \n")
            
            val result = resolver.resolveFile(passwordFile.absolutePath)
            
            assertThat(result).isEqualTo("secret-with-spaces")
        }
        
        @Test
        fun `expands tilde to home directory`() {
            // Create a test file in temp dir and mock the path
            val passwordFile = tempDir.resolve("password.txt").toFile()
            passwordFile.writeText("home-password")
            
            val resolverWithHome = CredentialResolver(homeDir = tempDir.toString())
            val result = resolverWithHome.resolveFile("~/password.txt")
            
            assertThat(result).isEqualTo("home-password")
        }
        
        @Test
        fun `throws when file not found`() {
            assertThatThrownBy {
                resolver.resolveFile("/nonexistent/path/password.txt")
            }.isInstanceOf(CredentialResolutionException::class.java)
             .hasMessageContaining("not found")
        }
        
        @Test
        fun `returns null for null path`() {
            val result = resolver.resolveFile(null)
            
            assertThat(result).isNull()
        }
    }
    
    @Nested
    @DisplayName("resolveEnvVar()")
    inner class ResolveEnvVarTest {
        
        @Test
        fun `resolves environment variable with dollar-brace syntax`() {
            val resolverWithEnv = CredentialResolver(
                envProvider = { name -> if (name == "MY_PASSWORD") "env-password" else null }
            )
            
            val result = resolverWithEnv.resolveEnvVar("\${MY_PASSWORD}")
            
            assertThat(result).isEqualTo("env-password")
        }
        
        @Test
        fun `returns original value when no env var syntax`() {
            val result = resolver.resolveEnvVar("plain-value")
            
            assertThat(result).isEqualTo("plain-value")
        }
        
        @Test
        fun `throws when env var not set`() {
            val resolverWithEnv = CredentialResolver(
                envProvider = { null }
            )
            
            assertThatThrownBy {
                resolverWithEnv.resolveEnvVar("\${MISSING_VAR}")
            }.isInstanceOf(CredentialResolutionException::class.java)
             .hasMessageContaining("MISSING_VAR")
             .hasMessageContaining("not set")
        }
        
        @Test
        fun `resolves multiple env vars in string`() {
            val resolverWithEnv = CredentialResolver(
                envProvider = { name ->
                    when (name) {
                        "USER" -> "admin"
                        "PASS" -> "secret"
                        else -> null
                    }
                }
            )
            
            val result = resolverWithEnv.resolveEnvVar("\${USER}:\${PASS}")
            
            assertThat(result).isEqualTo("admin:secret")
        }
        
        @Test
        fun `returns null for null input`() {
            val result = resolver.resolveEnvVar(null)
            
            assertThat(result).isNull()
        }
    }
    
    @Nested
    @DisplayName("resolvePassword()")
    inner class ResolvePasswordTest {
        
        @Test
        fun `prefers inline password over file`() {
            val passwordFile = tempDir.resolve("password.txt").toFile()
            passwordFile.writeText("file-password")
            
            val result = resolver.resolvePassword(
                inlinePassword = "inline-password",
                passwordFile = passwordFile.absolutePath
            )
            
            assertThat(result).isEqualTo("inline-password")
        }
        
        @Test
        fun `falls back to file when inline is null`() {
            val passwordFile = tempDir.resolve("password.txt").toFile()
            passwordFile.writeText("file-password")
            
            val result = resolver.resolvePassword(
                inlinePassword = null,
                passwordFile = passwordFile.absolutePath
            )
            
            assertThat(result).isEqualTo("file-password")
        }
        
        @Test
        fun `resolves env var in inline password`() {
            val resolverWithEnv = CredentialResolver(
                envProvider = { name -> if (name == "SECRET") "env-secret" else null }
            )
            
            val result = resolverWithEnv.resolvePassword(
                inlinePassword = "\${SECRET}",
                passwordFile = null
            )
            
            assertThat(result).isEqualTo("env-secret")
        }
        
        @Test
        fun `returns null when both inline and file are null`() {
            val result = resolver.resolvePassword(
                inlinePassword = null,
                passwordFile = null
            )
            
            assertThat(result).isNull()
        }
    }
    
    @Nested
    @DisplayName("expandPath()")
    inner class ExpandPathTest {
        
        @Test
        fun `expands tilde at start of path`() {
            val resolverWithHome = CredentialResolver(homeDir = "/home/user")
            
            val result = resolverWithHome.expandPath("~/.franz/config")
            
            assertThat(result).isEqualTo("/home/user/.franz/config")
        }
        
        @Test
        fun `does not expand tilde in middle of path`() {
            val resolverWithHome = CredentialResolver(homeDir = "/home/user")
            
            val result = resolverWithHome.expandPath("/path/to/~file")
            
            assertThat(result).isEqualTo("/path/to/~file")
        }
        
        @Test
        fun `returns absolute path unchanged`() {
            val result = resolver.expandPath("/etc/kafka/config")
            
            assertThat(result).isEqualTo("/etc/kafka/config")
        }
        
        @Test
        fun `returns null for null input`() {
            val result = resolver.expandPath(null)
            
            assertThat(result).isNull()
        }
    }
}
