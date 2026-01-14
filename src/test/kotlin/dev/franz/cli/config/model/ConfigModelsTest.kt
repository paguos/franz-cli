package dev.franz.cli.config.model

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Config Models")
class ConfigModelsTest {

    @Nested
    @DisplayName("FranzConfig")
    inner class FranzConfigTest {
        
        @Test
        fun `creates empty config with defaults`() {
            val config = FranzConfig()
            
            assertThat(config.apiVersion).isEqualTo("v1")
            assertThat(config.currentContext).isNull()
            assertThat(config.contexts).isEmpty()
            assertThat(config.clusters).isEmpty()
            assertThat(config.authConfigs).isEmpty()
        }
        
        @Test
        fun `parses minimal YAML config`() {
            val yaml = """
                apiVersion: v1
            """.trimIndent()
            
            val config = Yaml.default.decodeFromString<FranzConfig>(yaml)
            
            assertThat(config.apiVersion).isEqualTo("v1")
            assertThat(config.currentContext).isNull()
        }
        
        @Test
        fun `parses full YAML config`() {
            val yaml = """
                apiVersion: v1
                current-context: production
                contexts:
                  - name: production
                    cluster: prod-cluster
                    auth: prod-sasl
                clusters:
                  - name: prod-cluster
                    bootstrap-servers: kafka-prod:9093
                auth-configs:
                  - name: prod-sasl
                    security-protocol: SASL_SSL
                    sasl:
                      mechanism: SCRAM-SHA-512
                      username: admin
                      password: secret
            """.trimIndent()
            
            val config = Yaml.default.decodeFromString<FranzConfig>(yaml)
            
            assertThat(config.apiVersion).isEqualTo("v1")
            assertThat(config.currentContext).isEqualTo("production")
            assertThat(config.contexts).hasSize(1)
            assertThat(config.contexts[0].name).isEqualTo("production")
            assertThat(config.clusters).hasSize(1)
            assertThat(config.clusters[0].bootstrapServers).isEqualTo("kafka-prod:9093")
            assertThat(config.authConfigs).hasSize(1)
            assertThat(config.authConfigs[0].securityProtocol).isEqualTo(SecurityProtocol.SASL_SSL)
        }
        
        @Test
        fun `serializes to YAML`() {
            val config = FranzConfig(
                currentContext = "dev",
                contexts = listOf(
                    ContextEntry(name = "dev", cluster = "dev-cluster", auth = "dev-auth")
                ),
                clusters = listOf(
                    ClusterEntry(name = "dev-cluster", bootstrapServers = "localhost:9092")
                ),
                authConfigs = listOf(
                    AuthConfigEntry(name = "dev-auth", securityProtocol = SecurityProtocol.PLAINTEXT)
                )
            )
            
            val yaml = Yaml.default.encodeToString(config)
            
            assertThat(yaml).contains("current-context: \"dev\"")
            assertThat(yaml).contains("name: \"dev\"")
            assertThat(yaml).contains("bootstrap-servers: \"localhost:9092\"")
        }
    }
    
    @Nested
    @DisplayName("ContextEntry")
    inner class ContextEntryTest {
        
        @Test
        fun `creates context with required fields`() {
            val context = ContextEntry(name = "prod", cluster = "prod-cluster")
            
            assertThat(context.name).isEqualTo("prod")
            assertThat(context.cluster).isEqualTo("prod-cluster")
            assertThat(context.auth).isNull()
        }
        
        @Test
        fun `creates context with auth`() {
            val context = ContextEntry(name = "prod", cluster = "prod-cluster", auth = "prod-auth")
            
            assertThat(context.auth).isEqualTo("prod-auth")
        }
    }
    
    @Nested
    @DisplayName("ClusterEntry")
    inner class ClusterEntryTest {
        
        @Test
        fun `creates cluster with bootstrap servers`() {
            val cluster = ClusterEntry(name = "prod", bootstrapServers = "kafka-1:9092,kafka-2:9092")
            
            assertThat(cluster.name).isEqualTo("prod")
            assertThat(cluster.bootstrapServers).isEqualTo("kafka-1:9092,kafka-2:9092")
        }
    }
    
    @Nested
    @DisplayName("SecurityProtocol")
    inner class SecurityProtocolTest {
        
        @Test
        fun `parses PLAINTEXT`() {
            val yaml = """
                name: test
                security-protocol: PLAINTEXT
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            assertThat(auth.securityProtocol).isEqualTo(SecurityProtocol.PLAINTEXT)
        }
        
        @Test
        fun `parses SSL`() {
            val yaml = """
                name: test
                security-protocol: SSL
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            assertThat(auth.securityProtocol).isEqualTo(SecurityProtocol.SSL)
        }
        
        @Test
        fun `parses SASL_PLAINTEXT`() {
            val yaml = """
                name: test
                security-protocol: SASL_PLAINTEXT
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            assertThat(auth.securityProtocol).isEqualTo(SecurityProtocol.SASL_PLAINTEXT)
        }
        
        @Test
        fun `parses SASL_SSL`() {
            val yaml = """
                name: test
                security-protocol: SASL_SSL
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            assertThat(auth.securityProtocol).isEqualTo(SecurityProtocol.SASL_SSL)
        }
    }
    
    @Nested
    @DisplayName("SaslConfig")
    inner class SaslConfigTest {
        
        @Test
        fun `parses PLAIN mechanism with username and password`() {
            val yaml = """
                name: test
                security-protocol: SASL_PLAINTEXT
                sasl:
                  mechanism: PLAIN
                  username: user1
                  password: pass123
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.sasl).isNotNull
            assertThat(auth.sasl!!.mechanism).isEqualTo(SaslMechanism.PLAIN)
            assertThat(auth.sasl!!.username).isEqualTo("user1")
            assertThat(auth.sasl!!.password).isEqualTo("pass123")
        }
        
        @Test
        fun `parses SCRAM-SHA-256 mechanism`() {
            val yaml = """
                name: test
                security-protocol: SASL_PLAINTEXT
                sasl:
                  mechanism: SCRAM-SHA-256
                  username: admin
                  password: secret
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.sasl!!.mechanism).isEqualTo(SaslMechanism.SCRAM_SHA_256)
        }
        
        @Test
        fun `parses SCRAM-SHA-512 mechanism`() {
            val yaml = """
                name: test
                security-protocol: SASL_PLAINTEXT
                sasl:
                  mechanism: SCRAM-SHA-512
                  username: admin
                  password: secret
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.sasl!!.mechanism).isEqualTo(SaslMechanism.SCRAM_SHA_512)
        }
        
        @Test
        fun `parses GSSAPI (Kerberos) mechanism`() {
            val yaml = """
                name: test
                security-protocol: SASL_PLAINTEXT
                sasl:
                  mechanism: GSSAPI
                  principal: kafka/host@REALM
                  keytab: /etc/kafka/keytab
                  krb5-conf: /etc/krb5.conf
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.sasl!!.mechanism).isEqualTo(SaslMechanism.GSSAPI)
            assertThat(auth.sasl!!.principal).isEqualTo("kafka/host@REALM")
            assertThat(auth.sasl!!.keytab).isEqualTo("/etc/kafka/keytab")
            assertThat(auth.sasl!!.krb5Conf).isEqualTo("/etc/krb5.conf")
        }
        
        @Test
        fun `parses OAUTHBEARER mechanism`() {
            val yaml = """
                name: test
                security-protocol: SASL_PLAINTEXT
                sasl:
                  mechanism: OAUTHBEARER
                  token-endpoint: https://auth.example.com/oauth/token
                  client-id: kafka-client
                  client-secret: secret123
                  scope: kafka
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.sasl!!.mechanism).isEqualTo(SaslMechanism.OAUTHBEARER)
            assertThat(auth.sasl!!.tokenEndpoint).isEqualTo("https://auth.example.com/oauth/token")
            assertThat(auth.sasl!!.clientId).isEqualTo("kafka-client")
            assertThat(auth.sasl!!.clientSecret).isEqualTo("secret123")
            assertThat(auth.sasl!!.scope).isEqualTo("kafka")
        }
        
        @Test
        fun `parses password-file reference`() {
            val yaml = """
                name: test
                security-protocol: SASL_PLAINTEXT
                sasl:
                  mechanism: PLAIN
                  username: user1
                  password-file: ~/.franz/secrets/password
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.sasl!!.passwordFile).isEqualTo("~/.franz/secrets/password")
        }
    }
    
    @Nested
    @DisplayName("SslConfig")
    inner class SslConfigTest {
        
        @Test
        fun `parses SSL config with truststore`() {
            val yaml = """
                name: test
                security-protocol: SSL
                ssl:
                  truststore-location: /etc/kafka/truststore.jks
                  truststore-password: changeit
                  truststore-type: JKS
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.ssl).isNotNull
            assertThat(auth.ssl!!.truststoreLocation).isEqualTo("/etc/kafka/truststore.jks")
            assertThat(auth.ssl!!.truststorePassword).isEqualTo("changeit")
            assertThat(auth.ssl!!.truststoreType).isEqualTo("JKS")
        }
        
        @Test
        fun `parses SSL config with mTLS (keystore)`() {
            val yaml = """
                name: test
                security-protocol: SSL
                ssl:
                  truststore-location: /etc/kafka/truststore.jks
                  truststore-password: changeit
                  keystore-location: /etc/kafka/keystore.jks
                  keystore-password: changeit
                  key-password: keypass
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.ssl!!.keystoreLocation).isEqualTo("/etc/kafka/keystore.jks")
            assertThat(auth.ssl!!.keystorePassword).isEqualTo("changeit")
            assertThat(auth.ssl!!.keyPassword).isEqualTo("keypass")
        }
        
        @Test
        fun `uses default truststore type JKS`() {
            val yaml = """
                name: test
                security-protocol: SSL
                ssl:
                  truststore-location: /etc/kafka/truststore.jks
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.ssl!!.truststoreType).isEqualTo("JKS")
        }
        
        @Test
        fun `parses PKCS12 truststore type`() {
            val yaml = """
                name: test
                security-protocol: SSL
                ssl:
                  truststore-location: /etc/kafka/truststore.p12
                  truststore-type: PKCS12
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.ssl!!.truststoreType).isEqualTo("PKCS12")
        }
        
        @Test
        fun `parses SSL config with PEM files`() {
            val yaml = """
                name: test
                security-protocol: SSL
                ssl:
                  cafile: /etc/kafka/ca.crt
                  clientfile: /etc/kafka/client.crt
                  clientkeyfile: /etc/kafka/client.key
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.ssl).isNotNull
            assertThat(auth.ssl!!.caFile).isEqualTo("/etc/kafka/ca.crt")
            assertThat(auth.ssl!!.clientFile).isEqualTo("/etc/kafka/client.crt")
            assertThat(auth.ssl!!.clientKeyFile).isEqualTo("/etc/kafka/client.key")
        }
    }
    
    @Nested
    @DisplayName("Kafka properties passthrough")
    inner class KafkaPropertiesTest {
        
        @Test
        fun `parses kafka-properties map`() {
            val yaml = """
                name: test
                security-protocol: SSL
                kafka-properties:
                  ssl.endpoint.identification.algorithm: ""
                  request.timeout.ms: "12345"
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.kafkaProperties)
                .containsEntry("ssl.endpoint.identification.algorithm", "")
                .containsEntry("request.timeout.ms", "12345")
        }
    }
    
    @Nested
    @DisplayName("Combined SASL_SSL")
    inner class CombinedSaslSslTest {
        
        @Test
        fun `parses SASL_SSL with both SASL and SSL config`() {
            val yaml = """
                name: prod-auth
                security-protocol: SASL_SSL
                sasl:
                  mechanism: SCRAM-SHA-512
                  username: admin
                  password: secret
                ssl:
                  truststore-location: /etc/kafka/truststore.jks
                  truststore-password: changeit
            """.trimIndent()
            
            val auth = Yaml.default.decodeFromString<AuthConfigEntry>(yaml)
            
            assertThat(auth.securityProtocol).isEqualTo(SecurityProtocol.SASL_SSL)
            assertThat(auth.sasl).isNotNull
            assertThat(auth.sasl!!.mechanism).isEqualTo(SaslMechanism.SCRAM_SHA_512)
            assertThat(auth.ssl).isNotNull
            assertThat(auth.ssl!!.truststoreLocation).isEqualTo("/etc/kafka/truststore.jks")
        }
    }
}
