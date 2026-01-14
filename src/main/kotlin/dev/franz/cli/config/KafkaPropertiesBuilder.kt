package dev.franz.cli.config

import dev.franz.cli.config.model.SaslConfig
import dev.franz.cli.config.model.SaslMechanism
import dev.franz.cli.config.model.SecurityProtocol
import dev.franz.cli.config.model.SslConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import java.util.Properties

/**
 * Builds Kafka client Properties from a ResolvedContext.
 * Supports all native Kafka authentication methods.
 */
class KafkaPropertiesBuilder(
    private val credentialResolver: CredentialResolver = CredentialResolver()
) {
    private companion object {
        // Kafka PEM SSL config (KIP-651). We use string keys for compatibility across client versions.
        private const val SSL_TRUSTSTORE_CERTIFICATES = "ssl.truststore.certificates"
        private const val SSL_KEYSTORE_CERTIFICATE_CHAIN = "ssl.keystore.certificate.chain"
        private const val SSL_KEYSTORE_KEY = "ssl.keystore.key"
    }
    
    /**
     * Builds Kafka client properties from a resolved context.
     */
    fun build(context: ResolvedContext): Properties {
        return Properties().apply {
            // Bootstrap servers
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, context.bootstrapServers)
            
            // Security protocol
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, context.securityProtocol.name)
            
            // Admin client timeouts
            put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000)
            put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 30000)
            
            // Configure SSL if present
            context.ssl?.let { ssl ->
                configureSsl(this, ssl)
            }
            
            // Configure SASL if present
            context.sasl?.let { sasl ->
                configureSasl(this, sasl)
            }
            
            // Escape hatch overrides (last-wins)
            context.kafkaProperties.forEach { (k, v) ->
                // Allow ${ENV_VAR} expansion in values
                this[k] = credentialResolver.resolveEnvVar(v) ?: v
            }
        }
    }
    
    private fun configureSsl(props: Properties, ssl: SslConfig) {
        val pemMode = (ssl.caFile != null) || (ssl.clientFile != null) || (ssl.clientKeyFile != null)
        if (pemMode) {
            // Disallow mixing PEM-style config with keystore/truststore file config.
            val keystoreModeFields = listOf(
                ssl.truststoreLocation,
                ssl.truststorePassword,
                ssl.keystoreLocation,
                ssl.keystorePassword,
                ssl.keyPassword
            )
            if (keystoreModeFields.any { it != null }) {
                throw ConfigException(
                    "Invalid SSL config: PEM fields (cafile/clientfile/clientkeyfile) cannot be combined with " +
                        "truststore/keystore fields (truststore-location/keystore-location/etc.)."
                )
            }
            
            // If configuring mTLS, require both cert and key.
            if ((ssl.clientFile != null) xor (ssl.clientKeyFile != null)) {
                throw ConfigException("Invalid SSL config: both clientfile and clientkeyfile must be set for mTLS.")
            }
            
            // Trust (server validation)
            ssl.caFile?.let { caPath ->
                props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "PEM"
                props[SSL_TRUSTSTORE_CERTIFICATES] = credentialResolver.resolveFile(caPath)
            }
            
            // Client auth (mTLS)
            if (ssl.clientFile != null && ssl.clientKeyFile != null) {
                props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PEM"
                props[SSL_KEYSTORE_CERTIFICATE_CHAIN] = credentialResolver.resolveFile(ssl.clientFile)
                props[SSL_KEYSTORE_KEY] = credentialResolver.resolveFile(ssl.clientKeyFile)
            }
            
            return
        }
        
        ssl.truststoreLocation?.let {
            props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = credentialResolver.expandPath(it)
        }
        ssl.truststorePassword?.let {
            props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credentialResolver.resolveEnvVar(it)
        }
        props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = ssl.truststoreType
        
        // mTLS configuration (client certificate)
        ssl.keystoreLocation?.let {
            props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = credentialResolver.expandPath(it)
        }
        ssl.keystorePassword?.let {
            props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credentialResolver.resolveEnvVar(it)
        }
        ssl.keystoreType.let {
            props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = it
        }
        ssl.keyPassword?.let {
            props[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credentialResolver.resolveEnvVar(it)
        }
    }
    
    private fun configureSasl(props: Properties, sasl: SaslConfig) {
        // Set SASL mechanism
        props[SaslConfigs.SASL_MECHANISM] = when (sasl.mechanism) {
            SaslMechanism.PLAIN -> "PLAIN"
            SaslMechanism.SCRAM_SHA_256 -> "SCRAM-SHA-256"
            SaslMechanism.SCRAM_SHA_512 -> "SCRAM-SHA-512"
            SaslMechanism.GSSAPI -> "GSSAPI"
            SaslMechanism.OAUTHBEARER -> "OAUTHBEARER"
        }
        
        // Set JAAS config based on mechanism
        props[SaslConfigs.SASL_JAAS_CONFIG] = buildJaasConfig(sasl)
        
        // Mechanism-specific additional config
        when (sasl.mechanism) {
            SaslMechanism.GSSAPI -> {
                props[SaslConfigs.SASL_KERBEROS_SERVICE_NAME] = "kafka"
            }
            SaslMechanism.OAUTHBEARER -> {
                sasl.tokenEndpoint?.let {
                    props[SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL] = it
                }
            }
            else -> { /* No additional config needed */ }
        }
    }
    
    private fun buildJaasConfig(sasl: SaslConfig): String {
        return when (sasl.mechanism) {
            SaslMechanism.PLAIN -> buildPlainJaasConfig(sasl)
            SaslMechanism.SCRAM_SHA_256, SaslMechanism.SCRAM_SHA_512 -> buildScramJaasConfig(sasl)
            SaslMechanism.GSSAPI -> buildGssapiJaasConfig(sasl)
            SaslMechanism.OAUTHBEARER -> buildOauthbearerJaasConfig(sasl)
        }
    }
    
    private fun buildPlainJaasConfig(sasl: SaslConfig): String {
        val password = credentialResolver.resolvePassword(sasl.password, sasl.passwordFile)
            ?: throw ConfigException("Password required for SASL/PLAIN authentication")
        
        return """org.apache.kafka.common.security.plain.PlainLoginModule required username="${sasl.username}" password="$password";"""
    }
    
    private fun buildScramJaasConfig(sasl: SaslConfig): String {
        val password = credentialResolver.resolvePassword(sasl.password, sasl.passwordFile)
            ?: throw ConfigException("Password required for SASL/SCRAM authentication")
        
        return """org.apache.kafka.common.security.scram.ScramLoginModule required username="${sasl.username}" password="$password";"""
    }
    
    private fun buildGssapiJaasConfig(sasl: SaslConfig): String {
        val keytab = credentialResolver.expandPath(sasl.keytab)
            ?: throw ConfigException("Keytab required for GSSAPI/Kerberos authentication")
        
        return buildString {
            append("com.sun.security.auth.module.Krb5LoginModule required ")
            append("useKeyTab=true ")
            append("storeKey=true ")
            append("keyTab=\"$keytab\" ")
            append("principal=\"${sasl.principal}\";")
        }
    }
    
    private fun buildOauthbearerJaasConfig(sasl: SaslConfig): String {
        return buildString {
            append("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required ")
            sasl.clientId?.let { append("clientId=\"$it\" ") }
            sasl.clientSecret?.let { 
                val secret = credentialResolver.resolveEnvVar(it)
                append("clientSecret=\"$secret\" ")
            }
            sasl.scope?.let { append("scope=\"$it\" ") }
            append(";")
        }
    }
}
