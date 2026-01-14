package dev.franz.cli.config.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root configuration object for Franz CLI.
 * Stored at ~/.franz/config
 */
@Serializable
data class FranzConfig(
    @SerialName("apiVersion")
    val apiVersion: String = "v1",
    
    @SerialName("current-context")
    val currentContext: String? = null,
    
    @SerialName("contexts")
    val contexts: List<ContextEntry> = emptyList(),
    
    @SerialName("clusters")
    val clusters: List<ClusterEntry> = emptyList(),
    
    @SerialName("auth-configs")
    val authConfigs: List<AuthConfigEntry> = emptyList()
)

/**
 * A named context that references a cluster and optional auth configuration.
 */
@Serializable
data class ContextEntry(
    @SerialName("name")
    val name: String,
    
    @SerialName("cluster")
    val cluster: String,
    
    @SerialName("auth")
    val auth: String? = null
)

/**
 * Kafka cluster connection information.
 */
@Serializable
data class ClusterEntry(
    @SerialName("name")
    val name: String,
    
    @SerialName("bootstrap-servers")
    val bootstrapServers: String
)

/**
 * Authentication configuration for connecting to Kafka.
 */
@Serializable
data class AuthConfigEntry(
    @SerialName("name")
    val name: String,
    
    @SerialName("security-protocol")
    val securityProtocol: SecurityProtocol,
    
    @SerialName("sasl")
    val sasl: SaslConfig? = null,
    
    @SerialName("ssl")
    val ssl: SslConfig? = null,
    
    /**
     * Escape hatch for advanced Kafka client configuration.
     * These entries are merged into the final Kafka Properties (last-wins).
     */
    @SerialName("kafka-properties")
    val kafkaProperties: Map<String, String> = emptyMap()
)

/**
 * Kafka security protocols.
 */
@Serializable
enum class SecurityProtocol {
    PLAINTEXT,
    SSL,
    SASL_PLAINTEXT,
    SASL_SSL
}

/**
 * SASL authentication mechanisms.
 */
@Serializable
enum class SaslMechanism {
    PLAIN,
    @SerialName("SCRAM-SHA-256")
    SCRAM_SHA_256,
    @SerialName("SCRAM-SHA-512")
    SCRAM_SHA_512,
    GSSAPI,
    OAUTHBEARER
}

/**
 * SASL configuration options.
 * Supports multiple authentication mechanisms including PLAIN, SCRAM, Kerberos, and OAuth.
 */
@Serializable
data class SaslConfig(
    @SerialName("mechanism")
    val mechanism: SaslMechanism,
    
    // Username/password authentication (PLAIN, SCRAM)
    @SerialName("username")
    val username: String? = null,
    
    @SerialName("password")
    val password: String? = null,
    
    @SerialName("password-file")
    val passwordFile: String? = null,
    
    // Kerberos (GSSAPI) configuration
    @SerialName("principal")
    val principal: String? = null,
    
    @SerialName("keytab")
    val keytab: String? = null,
    
    @SerialName("krb5-conf")
    val krb5Conf: String? = null,
    
    // OAuth configuration (OAUTHBEARER)
    @SerialName("token-endpoint")
    val tokenEndpoint: String? = null,
    
    @SerialName("client-id")
    val clientId: String? = null,
    
    @SerialName("client-secret")
    val clientSecret: String? = null,
    
    @SerialName("scope")
    val scope: String? = null
)

/**
 * SSL/TLS configuration for encrypted connections.
 * Supports both server authentication (truststore) and mutual TLS (keystore).
 */
@Serializable
data class SslConfig(
    // Truststore configuration (server certificate validation)
    @SerialName("truststore-location")
    val truststoreLocation: String? = null,
    
    @SerialName("truststore-password")
    val truststorePassword: String? = null,
    
    @SerialName("truststore-type")
    val truststoreType: String = "JKS",
    
    // Keystore configuration (client certificate for mTLS)
    @SerialName("keystore-location")
    val keystoreLocation: String? = null,
    
    @SerialName("keystore-password")
    val keystorePassword: String? = null,
    
    @SerialName("keystore-type")
    val keystoreType: String = "JKS",
    
    @SerialName("key-password")
    val keyPassword: String? = null,
    
    /**
     * PEM configuration (KIP-651 style).
     * These are paths to PEM files and are mutually exclusive with keystore/truststore file options.
     */
    @SerialName("cafile")
    val caFile: String? = null,
    
    @SerialName("clientfile")
    val clientFile: String? = null,
    
    @SerialName("clientkeyfile")
    val clientKeyFile: String? = null
)
