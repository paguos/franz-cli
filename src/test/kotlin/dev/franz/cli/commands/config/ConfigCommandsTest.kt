package dev.franz.cli.commands.config

import dev.franz.cli.config.ConfigManager
import dev.franz.cli.config.model.*
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@DisplayName("Config Commands")
class ConfigCommandsTest {
    
    private lateinit var configManager: ConfigManager
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    
    @BeforeEach
    fun setUp() {
        configManager = mockk(relaxed = true)
        outputStream = ByteArrayOutputStream()
        originalOut = System.out
        System.setOut(PrintStream(outputStream))
    }
    
    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
    }
    
    @Nested
    @DisplayName("GetContexts")
    inner class GetContextsTest {
        
        @Test
        fun `lists all contexts with current marked`() {
            every { configManager.loadConfig() } returns FranzConfig(
                currentContext = "prod",
                contexts = listOf(
                    ContextEntry("dev", "dev-cluster"),
                    ContextEntry("prod", "prod-cluster")
                )
            )
            
            GetContexts(configManager).main(emptyArray())
            
            val output = outputStream.toString()
            assertThat(output).contains("dev")
            assertThat(output).contains("prod")
            assertThat(output).contains("*") // current context marker
        }
        
        @Test
        fun `shows empty message when no contexts`() {
            every { configManager.loadConfig() } returns FranzConfig()
            
            GetContexts(configManager).main(emptyArray())
            
            val output = outputStream.toString()
            assertThat(output).containsIgnoringCase("no contexts")
        }
    }
    
    @Nested
    @DisplayName("UseContext")
    inner class UseContextTest {
        
        @Test
        fun `switches current context`() {
            every { configManager.setCurrentContext("prod") } just runs
            
            UseContext(configManager).main(arrayOf("prod"))
            
            verify { configManager.setCurrentContext("prod") }
            assertThat(outputStream.toString()).contains("Switched to context \"prod\"")
        }
    }
    
    @Nested
    @DisplayName("CurrentContext")
    inner class CurrentContextTest {
        
        @Test
        fun `shows current context name`() {
            every { configManager.getCurrentContextName() } returns "production"
            
            CurrentContext(configManager).main(emptyArray())
            
            assertThat(outputStream.toString()).contains("production")
        }
        
        @Test
        fun `shows message when no context set`() {
            every { configManager.getCurrentContextName() } returns null
            
            CurrentContext(configManager).main(emptyArray())
            
            assertThat(outputStream.toString()).containsIgnoringCase("no current context")
        }
    }
    
    @Nested
    @DisplayName("SetContext")
    inner class SetContextTest {
        
        @Test
        fun `creates new context`() {
            val contextSlot = slot<ContextEntry>()
            every { configManager.setContext(capture(contextSlot)) } just runs
            
            SetContext(configManager).main(arrayOf(
                "my-context",
                "--cluster", "my-cluster",
                "--auth", "my-auth"
            ))
            
            assertThat(contextSlot.captured.name).isEqualTo("my-context")
            assertThat(contextSlot.captured.cluster).isEqualTo("my-cluster")
            assertThat(contextSlot.captured.auth).isEqualTo("my-auth")
        }
        
        @Test
        fun `creates context without auth`() {
            val contextSlot = slot<ContextEntry>()
            every { configManager.setContext(capture(contextSlot)) } just runs
            
            SetContext(configManager).main(arrayOf(
                "my-context",
                "--cluster", "my-cluster"
            ))
            
            assertThat(contextSlot.captured.auth).isNull()
        }
    }
    
    @Nested
    @DisplayName("SetCluster")
    inner class SetClusterTest {
        
        @Test
        fun `creates cluster with bootstrap-servers`() {
            val clusterSlot = slot<ClusterEntry>()
            every { configManager.setCluster(capture(clusterSlot)) } just runs
            
            SetCluster(configManager).main(arrayOf(
                "my-cluster",
                "--bootstrap-servers", "kafka:9092"
            ))
            
            assertThat(clusterSlot.captured.name).isEqualTo("my-cluster")
            assertThat(clusterSlot.captured.bootstrapServers).isEqualTo("kafka:9092")
        }
    }
    
    @Nested
    @DisplayName("SetCredentials")
    inner class SetCredentialsTest {
        
        @Test
        fun `creates PLAINTEXT auth config`() {
            val authSlot = slot<AuthConfigEntry>()
            every { configManager.setAuthConfig(capture(authSlot)) } just runs
            
            SetCredentials(configManager).main(arrayOf(
                "my-auth",
                "--security-protocol", "PLAINTEXT"
            ))
            
            assertThat(authSlot.captured.name).isEqualTo("my-auth")
            assertThat(authSlot.captured.securityProtocol).isEqualTo(SecurityProtocol.PLAINTEXT)
        }
        
        @Test
        fun `creates SASL_PLAIN auth config`() {
            val authSlot = slot<AuthConfigEntry>()
            every { configManager.setAuthConfig(capture(authSlot)) } just runs
            
            SetCredentials(configManager).main(arrayOf(
                "my-auth",
                "--security-protocol", "SASL_PLAINTEXT",
                "--sasl-mechanism", "PLAIN",
                "--username", "admin",
                "--password", "secret"
            ))
            
            assertThat(authSlot.captured.securityProtocol).isEqualTo(SecurityProtocol.SASL_PLAINTEXT)
            assertThat(authSlot.captured.sasl?.mechanism).isEqualTo(SaslMechanism.PLAIN)
            assertThat(authSlot.captured.sasl?.username).isEqualTo("admin")
            assertThat(authSlot.captured.sasl?.password).isEqualTo("secret")
        }
    }
    
    @Nested
    @DisplayName("DeleteContext")
    inner class DeleteContextTest {
        
        @Test
        fun `deletes context`() {
            every { configManager.deleteContext("old-context") } returns true
            
            DeleteContext(configManager).main(arrayOf("old-context"))
            
            verify { configManager.deleteContext("old-context") }
            assertThat(outputStream.toString()).contains("deleted")
        }
        
        @Test
        fun `shows message when context not found`() {
            every { configManager.deleteContext("nonexistent") } returns false
            
            DeleteContext(configManager).main(arrayOf("nonexistent"))
            
            assertThat(outputStream.toString()).containsIgnoringCase("not found")
        }
    }
    
    @Nested
    @DisplayName("ViewConfig")
    inner class ViewConfigTest {
        
        @Test
        fun `displays config with redacted secrets`() {
            every { configManager.loadConfig() } returns FranzConfig(
                currentContext = "dev",
                contexts = listOf(ContextEntry("dev", "dev-cluster", "dev-auth")),
                clusters = listOf(ClusterEntry("dev-cluster", "localhost:9092")),
                authConfigs = listOf(
                    AuthConfigEntry(
                        name = "dev-auth",
                        securityProtocol = SecurityProtocol.SASL_PLAINTEXT,
                        sasl = SaslConfig(
                            mechanism = SaslMechanism.PLAIN,
                            username = "admin",
                            password = "super-secret-password"
                        )
                    )
                )
            )
            
            ViewConfig(configManager).main(emptyArray())
            
            val output = outputStream.toString()
            assertThat(output).contains("dev")
            assertThat(output).contains("localhost:9092")
            assertThat(output).contains("admin")
            // Password should be redacted
            assertThat(output).doesNotContain("super-secret-password")
            assertThat(output).contains("***")
        }
    }
}
