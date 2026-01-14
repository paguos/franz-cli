package dev.franz.cli.config

import dev.franz.cli.config.model.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@DisplayName("ConfigManager")
class ConfigManagerTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var configManager: ConfigManager
    private lateinit var configPath: Path
    
    @BeforeEach
    fun setUp() {
        configPath = tempDir.resolve(".franz").resolve("config")
        configManager = ConfigManager(configPath = configPath)
    }
    
    @Nested
    @DisplayName("Loading Config")
    inner class LoadingTest {
        
        @Test
        fun `returns empty config when file does not exist`() {
            val config = configManager.loadConfig()
            
            assertThat(config).isNotNull
            assertThat(config.apiVersion).isEqualTo("v1")
            assertThat(config.currentContext).isNull()
            assertThat(config.contexts).isEmpty()
            assertThat(config.clusters).isEmpty()
            assertThat(config.authConfigs).isEmpty()
        }
        
        @Test
        fun `loads config from YAML file`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                current-context: dev
                contexts:
                  - name: dev
                    cluster: dev-cluster
                    auth: dev-auth
                clusters:
                  - name: dev-cluster
                    bootstrap-servers: localhost:9092
                auth-configs:
                  - name: dev-auth
                    security-protocol: PLAINTEXT
            """.trimIndent())
            
            val config = configManager.loadConfig()
            
            assertThat(config.currentContext).isEqualTo("dev")
            assertThat(config.contexts).hasSize(1)
            assertThat(config.clusters).hasSize(1)
            assertThat(config.authConfigs).hasSize(1)
        }
        
        @Test
        fun `returns current context name`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                current-context: production
            """.trimIndent())
            
            val currentContext = configManager.getCurrentContextName()
            
            assertThat(currentContext).isEqualTo("production")
        }
        
        @Test
        fun `returns null when no current context set`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
            """.trimIndent())
            
            val currentContext = configManager.getCurrentContextName()
            
            assertThat(currentContext).isNull()
        }
        
        @Test
        fun `finds context by name`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: dev-cluster
                  - name: prod
                    cluster: prod-cluster
            """.trimIndent())
            
            val context = configManager.getContext("prod")
            
            assertThat(context).isNotNull
            assertThat(context!!.name).isEqualTo("prod")
            assertThat(context.cluster).isEqualTo("prod-cluster")
        }
        
        @Test
        fun `returns null for non-existent context`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: dev-cluster
            """.trimIndent())
            
            val context = configManager.getContext("nonexistent")
            
            assertThat(context).isNull()
        }
        
        @Test
        fun `finds cluster by name`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                clusters:
                  - name: dev-cluster
                    bootstrap-servers: localhost:9092
                  - name: prod-cluster
                    bootstrap-servers: kafka-prod:9093
            """.trimIndent())
            
            val cluster = configManager.getCluster("prod-cluster")
            
            assertThat(cluster).isNotNull
            assertThat(cluster!!.name).isEqualTo("prod-cluster")
            assertThat(cluster.bootstrapServers).isEqualTo("kafka-prod:9093")
        }
        
        @Test
        fun `finds auth config by name`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                auth-configs:
                  - name: dev-auth
                    security-protocol: PLAINTEXT
                  - name: prod-auth
                    security-protocol: SASL_SSL
                    sasl:
                      mechanism: SCRAM-SHA-512
                      username: admin
            """.trimIndent())
            
            val auth = configManager.getAuthConfig("prod-auth")
            
            assertThat(auth).isNotNull
            assertThat(auth!!.name).isEqualTo("prod-auth")
            assertThat(auth.securityProtocol).isEqualTo(SecurityProtocol.SASL_SSL)
            assertThat(auth.sasl?.mechanism).isEqualTo(SaslMechanism.SCRAM_SHA_512)
        }
    }
    
    @Nested
    @DisplayName("Resolving Context")
    inner class ResolvingContextTest {
        
        @Test
        fun `resolves context to cluster and auth config`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                current-context: dev
                contexts:
                  - name: dev
                    cluster: dev-cluster
                    auth: dev-auth
                clusters:
                  - name: dev-cluster
                    bootstrap-servers: localhost:9092
                auth-configs:
                  - name: dev-auth
                    security-protocol: PLAINTEXT
            """.trimIndent())
            
            val resolved = configManager.resolveContext("dev")
            
            assertThat(resolved).isNotNull
            assertThat(resolved.contextName).isEqualTo("dev")
            assertThat(resolved.bootstrapServers).isEqualTo("localhost:9092")
            assertThat(resolved.securityProtocol).isEqualTo(SecurityProtocol.PLAINTEXT)
        }
        
        @Test
        fun `resolves context without auth config`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: dev-cluster
                clusters:
                  - name: dev-cluster
                    bootstrap-servers: localhost:9092
            """.trimIndent())
            
            val resolved = configManager.resolveContext("dev")
            
            assertThat(resolved).isNotNull
            assertThat(resolved.securityProtocol).isEqualTo(SecurityProtocol.PLAINTEXT)
            assertThat(resolved.sasl).isNull()
            assertThat(resolved.ssl).isNull()
        }
        
        @Test
        fun `resolves current context when name is null`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                current-context: dev
                contexts:
                  - name: dev
                    cluster: dev-cluster
                clusters:
                  - name: dev-cluster
                    bootstrap-servers: localhost:9092
            """.trimIndent())
            
            val resolved = configManager.resolveContext(null)
            
            assertThat(resolved).isNotNull
            assertThat(resolved.contextName).isEqualTo("dev")
        }
        
        @Test
        fun `throws when context not found`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: dev-cluster
            """.trimIndent())
            
            assertThatThrownBy {
                configManager.resolveContext("nonexistent")
            }.isInstanceOf(ConfigException::class.java)
             .hasMessageContaining("Context 'nonexistent' not found")
        }
        
        @Test
        fun `throws when no current context and name is null`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
            """.trimIndent())
            
            assertThatThrownBy {
                configManager.resolveContext(null)
            }.isInstanceOf(ConfigException::class.java)
             .hasMessageContaining("No current context")
        }
        
        @Test
        fun `throws when cluster not found`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: nonexistent-cluster
            """.trimIndent())
            
            assertThatThrownBy {
                configManager.resolveContext("dev")
            }.isInstanceOf(ConfigException::class.java)
             .hasMessageContaining("Cluster 'nonexistent-cluster' not found")
        }
        
        @Test
        fun `throws when auth config not found`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: dev-cluster
                    auth: nonexistent-auth
                clusters:
                  - name: dev-cluster
                    bootstrap-servers: localhost:9092
            """.trimIndent())
            
            assertThatThrownBy {
                configManager.resolveContext("dev")
            }.isInstanceOf(ConfigException::class.java)
             .hasMessageContaining("Auth config 'nonexistent-auth' not found")
        }
    }
    
    @Nested
    @DisplayName("Saving Config")
    inner class SavingTest {
        
        @Test
        fun `saves config to YAML file`() {
            val config = FranzConfig(
                currentContext = "dev",
                contexts = listOf(ContextEntry("dev", "dev-cluster")),
                clusters = listOf(ClusterEntry("dev-cluster", "localhost:9092"))
            )
            
            configManager.saveConfig(config)
            
            val savedContent = configPath.toFile().readText()
            assertThat(savedContent).contains("current-context")
            assertThat(savedContent).contains("dev")
            assertThat(savedContent).contains("localhost:9092")
        }
        
        @Test
        fun `creates directory if not exists`() {
            val config = FranzConfig(currentContext = "dev")
            
            configManager.saveConfig(config)
            
            assertThat(configPath.toFile()).exists()
            assertThat(configPath.parent.toFile()).isDirectory
        }
        
        @Test
        fun `sets current context`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: dev-cluster
                  - name: prod
                    cluster: prod-cluster
            """.trimIndent())
            
            configManager.setCurrentContext("prod")
            
            val currentContext = configManager.getCurrentContextName()
            assertThat(currentContext).isEqualTo("prod")
        }
        
        @Test
        fun `throws when setting non-existent context as current`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: dev-cluster
            """.trimIndent())
            
            assertThatThrownBy {
                configManager.setCurrentContext("nonexistent")
            }.isInstanceOf(ConfigException::class.java)
             .hasMessageContaining("not found")
        }
        
        @Test
        fun `adds new context`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: dev-cluster
            """.trimIndent())
            
            configManager.setContext(ContextEntry("prod", "prod-cluster", "prod-auth"))
            
            val contexts = configManager.listContexts()
            assertThat(contexts).hasSize(2)
            assertThat(contexts.find { it.name == "prod" }?.auth).isEqualTo("prod-auth")
        }
        
        @Test
        fun `updates existing context`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: old-cluster
            """.trimIndent())
            
            configManager.setContext(ContextEntry("dev", "new-cluster", "new-auth"))
            
            val context = configManager.getContext("dev")
            assertThat(context?.cluster).isEqualTo("new-cluster")
            assertThat(context?.auth).isEqualTo("new-auth")
        }
        
        @Test
        fun `adds new cluster`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
            """.trimIndent())
            
            configManager.setCluster(ClusterEntry("new-cluster", "kafka:9092"))
            
            val cluster = configManager.getCluster("new-cluster")
            assertThat(cluster?.bootstrapServers).isEqualTo("kafka:9092")
        }
        
        @Test
        fun `adds new auth config`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
            """.trimIndent())
            
            configManager.setAuthConfig(
                AuthConfigEntry("new-auth", SecurityProtocol.SASL_SSL)
            )
            
            val auth = configManager.getAuthConfig("new-auth")
            assertThat(auth?.securityProtocol).isEqualTo(SecurityProtocol.SASL_SSL)
        }
        
        @Test
        fun `deletes context`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                contexts:
                  - name: dev
                    cluster: dev-cluster
                  - name: prod
                    cluster: prod-cluster
            """.trimIndent())
            
            val deleted = configManager.deleteContext("dev")
            
            assertThat(deleted).isTrue()
            assertThat(configManager.listContexts()).hasSize(1)
            assertThat(configManager.getContext("dev")).isNull()
        }
        
        @Test
        fun `clears current context when deleted`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                current-context: dev
                contexts:
                  - name: dev
                    cluster: dev-cluster
            """.trimIndent())
            
            configManager.deleteContext("dev")
            
            assertThat(configManager.getCurrentContextName()).isNull()
        }
        
        @Test
        fun `returns false when deleting non-existent context`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
            """.trimIndent())
            
            val deleted = configManager.deleteContext("nonexistent")
            
            assertThat(deleted).isFalse()
        }
    }
    
    @Nested
    @DisplayName("List Operations")
    inner class ListOperationsTest {
        
        @Test
        fun `lists all contexts`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                current-context: dev
                contexts:
                  - name: dev
                    cluster: dev-cluster
                  - name: prod
                    cluster: prod-cluster
            """.trimIndent())
            
            val contexts = configManager.listContexts()
            
            assertThat(contexts).hasSize(2)
            assertThat(contexts.map { it.name }).containsExactly("dev", "prod")
        }
        
        @Test
        fun `lists all clusters`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                clusters:
                  - name: dev-cluster
                    bootstrap-servers: localhost:9092
                  - name: prod-cluster
                    bootstrap-servers: kafka-prod:9093
            """.trimIndent())
            
            val clusters = configManager.listClusters()
            
            assertThat(clusters).hasSize(2)
            assertThat(clusters.map { it.name }).containsExactly("dev-cluster", "prod-cluster")
        }
        
        @Test
        fun `lists all auth configs`() {
            configPath.parent.toFile().mkdirs()
            configPath.toFile().writeText("""
                apiVersion: v1
                auth-configs:
                  - name: dev-auth
                    security-protocol: PLAINTEXT
                  - name: prod-auth
                    security-protocol: SASL_SSL
            """.trimIndent())
            
            val authConfigs = configManager.listAuthConfigs()
            
            assertThat(authConfigs).hasSize(2)
            assertThat(authConfigs.map { it.name }).containsExactly("dev-auth", "prod-auth")
        }
    }
}
