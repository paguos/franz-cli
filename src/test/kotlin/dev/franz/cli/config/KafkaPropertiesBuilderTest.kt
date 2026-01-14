package dev.franz.cli.config

import dev.franz.cli.config.model.*
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@DisplayName("KafkaPropertiesBuilder")
class KafkaPropertiesBuilderTest {
    
    private lateinit var builder: KafkaPropertiesBuilder
    
    @TempDir
    lateinit var tempDir: Path
    
    @BeforeEach
    fun setUp() {
        builder = KafkaPropertiesBuilder(
            credentialResolver = CredentialResolver(homeDir = tempDir.toString())
        )
    }
    
    @Nested
    @DisplayName("PLAINTEXT")
    inner class PlaintextTest {
        
        @Test
        fun `builds properties for PLAINTEXT`() {
            val context = ResolvedContext(
                contextName = "dev",
                bootstrapServers = "localhost:9092",
                securityProtocol = SecurityProtocol.PLAINTEXT
            )
            
            val props = builder.build(context)
            
            assertThat(props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG])
                .isEqualTo("localhost:9092")
            assertThat(props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG])
                .isEqualTo("PLAINTEXT")
        }
    }
    
    @Nested
    @DisplayName("SSL")
    inner class SslTest {
        
        @Test
        fun `builds properties for SSL with truststore`() {
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9093",
                securityProtocol = SecurityProtocol.SSL,
                ssl = SslConfig(
                    truststoreLocation = "/etc/kafka/truststore.jks",
                    truststorePassword = "changeit",
                    truststoreType = "JKS"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG])
                .isEqualTo("SSL")
            assertThat(props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG])
                .isEqualTo("/etc/kafka/truststore.jks")
            assertThat(props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG])
                .isEqualTo("changeit")
            assertThat(props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG])
                .isEqualTo("JKS")
        }
        
        @Test
        fun `builds properties for SSL with mTLS (keystore)`() {
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9093",
                securityProtocol = SecurityProtocol.SSL,
                ssl = SslConfig(
                    truststoreLocation = "/etc/kafka/truststore.jks",
                    truststorePassword = "trustpass",
                    keystoreLocation = "/etc/kafka/keystore.jks",
                    keystorePassword = "keystorepass",
                    keyPassword = "keypass"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG])
                .isEqualTo("/etc/kafka/keystore.jks")
            assertThat(props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG])
                .isEqualTo("keystorepass")
            assertThat(props[SslConfigs.SSL_KEY_PASSWORD_CONFIG])
                .isEqualTo("keypass")
        }
        
        @Test
        fun `builds properties for PKCS12 truststore`() {
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9093",
                securityProtocol = SecurityProtocol.SSL,
                ssl = SslConfig(
                    truststoreLocation = "/etc/kafka/truststore.p12",
                    truststorePassword = "changeit",
                    truststoreType = "PKCS12"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG])
                .isEqualTo("PKCS12")
        }
        
        @Test
        fun `builds properties for SSL with PEM files`() {
            val caFile = tempDir.resolve("ca.crt").toFile().apply { writeText("-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n") }
            val clientCrt = tempDir.resolve("client.crt").toFile().apply { writeText("-----BEGIN CERTIFICATE-----\nCLIENT\n-----END CERTIFICATE-----\n") }
            val clientKey = tempDir.resolve("client.key").toFile().apply { writeText("-----BEGIN PRIVATE KEY-----\nKEY\n-----END PRIVATE KEY-----\n") }
            
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9093",
                securityProtocol = SecurityProtocol.SSL,
                ssl = SslConfig(
                    caFile = caFile.absolutePath,
                    clientFile = clientCrt.absolutePath,
                    clientKeyFile = clientKey.absolutePath
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG]).isEqualTo("SSL")
            assertThat(props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG]).isEqualTo("PEM")
            assertThat(props["ssl.truststore.certificates"].toString()).contains("BEGIN CERTIFICATE")
            assertThat(props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG]).isEqualTo("PEM")
            assertThat(props["ssl.keystore.certificate.chain"].toString()).contains("BEGIN CERTIFICATE")
            assertThat(props["ssl.keystore.key"].toString()).contains("BEGIN PRIVATE KEY")
        }
        
        @Test
        fun `rejects mixing PEM config with keystore config`() {
            val caFile = tempDir.resolve("ca.crt").toFile().apply { writeText("CA") }
            
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9093",
                securityProtocol = SecurityProtocol.SSL,
                ssl = SslConfig(
                    caFile = caFile.absolutePath,
                    truststoreLocation = "/etc/kafka/truststore.jks"
                )
            )
            
            assertThatThrownBy { builder.build(context) }
                .isInstanceOf(ConfigException::class.java)
                .hasMessageContaining("cannot be combined")
        }
    }
    
    @Nested
    @DisplayName("SASL_PLAIN")
    inner class SaslPlainTest {
        
        @Test
        fun `builds properties for SASL_PLAIN with inline credentials`() {
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9092",
                securityProtocol = SecurityProtocol.SASL_PLAINTEXT,
                sasl = SaslConfig(
                    mechanism = SaslMechanism.PLAIN,
                    username = "admin",
                    password = "secret"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG])
                .isEqualTo("SASL_PLAINTEXT")
            assertThat(props[SaslConfigs.SASL_MECHANISM])
                .isEqualTo("PLAIN")
            assertThat(props[SaslConfigs.SASL_JAAS_CONFIG].toString())
                .contains("PlainLoginModule")
                .contains("username=\"admin\"")
                .contains("password=\"secret\"")
        }
        
        @Test
        fun `builds properties with password from file`() {
            val passwordFile = tempDir.resolve("password.txt").toFile()
            passwordFile.writeText("file-secret")
            
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9092",
                securityProtocol = SecurityProtocol.SASL_PLAINTEXT,
                sasl = SaslConfig(
                    mechanism = SaslMechanism.PLAIN,
                    username = "admin",
                    passwordFile = "~/password.txt"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[SaslConfigs.SASL_JAAS_CONFIG].toString())
                .contains("password=\"file-secret\"")
        }
    }
    
    @Nested
    @DisplayName("SASL_SCRAM")
    inner class SaslScramTest {
        
        @Test
        fun `builds properties for SASL_SCRAM_SHA_256`() {
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9092",
                securityProtocol = SecurityProtocol.SASL_PLAINTEXT,
                sasl = SaslConfig(
                    mechanism = SaslMechanism.SCRAM_SHA_256,
                    username = "admin",
                    password = "secret"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[SaslConfigs.SASL_MECHANISM])
                .isEqualTo("SCRAM-SHA-256")
            assertThat(props[SaslConfigs.SASL_JAAS_CONFIG].toString())
                .contains("ScramLoginModule")
        }
        
        @Test
        fun `builds properties for SASL_SCRAM_SHA_512`() {
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9092",
                securityProtocol = SecurityProtocol.SASL_PLAINTEXT,
                sasl = SaslConfig(
                    mechanism = SaslMechanism.SCRAM_SHA_512,
                    username = "admin",
                    password = "secret"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[SaslConfigs.SASL_MECHANISM])
                .isEqualTo("SCRAM-SHA-512")
        }
    }
    
    @Nested
    @DisplayName("SASL_GSSAPI (Kerberos)")
    inner class SaslGssapiTest {
        
        @Test
        fun `builds properties for GSSAPI (Kerberos)`() {
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9092",
                securityProtocol = SecurityProtocol.SASL_PLAINTEXT,
                sasl = SaslConfig(
                    mechanism = SaslMechanism.GSSAPI,
                    principal = "kafka/host@REALM",
                    keytab = "/etc/kafka/keytab",
                    krb5Conf = "/etc/krb5.conf"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[SaslConfigs.SASL_MECHANISM])
                .isEqualTo("GSSAPI")
            assertThat(props[SaslConfigs.SASL_JAAS_CONFIG].toString())
                .contains("Krb5LoginModule")
                .contains("principal=\"kafka/host@REALM\"")
                .contains("keyTab=\"/etc/kafka/keytab\"")
            assertThat(props[SaslConfigs.SASL_KERBEROS_SERVICE_NAME])
                .isEqualTo("kafka")
        }
    }
    
    @Nested
    @DisplayName("SASL_OAUTHBEARER")
    inner class SaslOauthbearerTest {
        
        @Test
        fun `builds properties for OAUTHBEARER`() {
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9092",
                securityProtocol = SecurityProtocol.SASL_PLAINTEXT,
                sasl = SaslConfig(
                    mechanism = SaslMechanism.OAUTHBEARER,
                    tokenEndpoint = "https://auth.example.com/oauth/token",
                    clientId = "kafka-client",
                    clientSecret = "secret123",
                    scope = "kafka"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[SaslConfigs.SASL_MECHANISM])
                .isEqualTo("OAUTHBEARER")
            assertThat(props[SaslConfigs.SASL_JAAS_CONFIG].toString())
                .contains("OAuthBearerLoginModule")
            assertThat(props[SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL])
                .isEqualTo("https://auth.example.com/oauth/token")
        }
    }
    
    @Nested
    @DisplayName("SASL_SSL")
    inner class SaslSslTest {
        
        @Test
        fun `builds properties for SASL_SSL combination`() {
            val context = ResolvedContext(
                contextName = "prod",
                bootstrapServers = "kafka:9093",
                securityProtocol = SecurityProtocol.SASL_SSL,
                sasl = SaslConfig(
                    mechanism = SaslMechanism.SCRAM_SHA_512,
                    username = "admin",
                    password = "secret"
                ),
                ssl = SslConfig(
                    truststoreLocation = "/etc/kafka/truststore.jks",
                    truststorePassword = "changeit"
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG])
                .isEqualTo("SASL_SSL")
            // SASL config
            assertThat(props[SaslConfigs.SASL_MECHANISM])
                .isEqualTo("SCRAM-SHA-512")
            assertThat(props[SaslConfigs.SASL_JAAS_CONFIG].toString())
                .contains("ScramLoginModule")
            // SSL config
            assertThat(props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG])
                .isEqualTo("/etc/kafka/truststore.jks")
            assertThat(props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG])
                .isEqualTo("changeit")
        }
    }
    
    @Nested
    @DisplayName("Admin Client Defaults")
    inner class AdminClientDefaultsTest {
        
        @Test
        fun `sets request timeout`() {
            val context = ResolvedContext(
                contextName = "dev",
                bootstrapServers = "localhost:9092",
                securityProtocol = SecurityProtocol.PLAINTEXT
            )
            
            val props = builder.build(context)
            
            assertThat(props["request.timeout.ms"]).isEqualTo(10000)
            assertThat(props["default.api.timeout.ms"]).isEqualTo(30000)
        }
        
        @Test
        fun `applies kafka-properties passthrough as last-wins`() {
            val context = ResolvedContext(
                contextName = "dev",
                bootstrapServers = "localhost:9092",
                securityProtocol = SecurityProtocol.PLAINTEXT,
                kafkaProperties = mapOf(
                    "request.timeout.ms" to "111",
                    "ssl.endpoint.identification.algorithm" to ""
                )
            )
            
            val props = builder.build(context)
            
            assertThat(props["request.timeout.ms"]).isEqualTo("111")
            assertThat(props["ssl.endpoint.identification.algorithm"]).isEqualTo("")
        }
    }
}
