package dev.franz.cli.commands.config

import dev.franz.cli.FranzCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import dev.franz.cli.config.ConfigManager
import dev.franz.cli.config.model.*

class SetCredentials(
    private val configManager: ConfigManager = ConfigManager()
) : FranzCommand(
    name = "set-credentials",
    help = """
        Create or update authentication credentials.

        Notes:
          - For SASL, set `--sasl-mechanism` and the matching fields (e.g. `--username` / `--password`).
          - For SSL/mTLS, provide truststore/keystore options as needed.

        Examples:
        ```
        franz config set-credentials local --security-protocol PLAINTEXT
        franz config set-credentials prod --security-protocol SASL_SSL --sasl-mechanism SCRAM-SHA-512 --username alice --password-file ./pw.txt
        ```
    """.trimIndent()
) {
    private val name by argument(help = "Name of the auth configuration")
    
    private val securityProtocol by option(
        "--security-protocol", "-p",
        help = "Security protocol"
    ).choice(
        "PLAINTEXT" to SecurityProtocol.PLAINTEXT,
        "SSL" to SecurityProtocol.SSL,
        "SASL_PLAINTEXT" to SecurityProtocol.SASL_PLAINTEXT,
        "SASL_SSL" to SecurityProtocol.SASL_SSL
    ).default(SecurityProtocol.PLAINTEXT)
    
    // SASL options
    private val saslMechanism by option(
        "--sasl-mechanism",
        help = "SASL mechanism"
    ).choice(
        "PLAIN" to SaslMechanism.PLAIN,
        "SCRAM-SHA-256" to SaslMechanism.SCRAM_SHA_256,
        "SCRAM-SHA-512" to SaslMechanism.SCRAM_SHA_512,
        "GSSAPI" to SaslMechanism.GSSAPI,
        "OAUTHBEARER" to SaslMechanism.OAUTHBEARER
    )
    
    private val username by option("--username", "-u", help = "SASL username")
    private val password by option("--password", help = "SASL password")
    private val passwordFile by option("--password-file", help = "Path to password file")
    
    // Kerberos options
    private val principal by option("--principal", help = "Kerberos principal")
    private val keytab by option("--keytab", help = "Path to Kerberos keytab")
    private val krb5Conf by option("--krb5-conf", help = "Path to krb5.conf")
    
    // OAuth options
    private val tokenEndpoint by option("--token-endpoint", help = "OAuth token endpoint URL")
    private val clientId by option("--client-id", help = "OAuth client ID")
    private val clientSecret by option("--client-secret", help = "OAuth client secret")
    private val scope by option("--scope", help = "OAuth scope")
    
    // SSL options
    private val truststoreLocation by option("--truststore-location", help = "Path to truststore")
    private val truststorePassword by option("--truststore-password", help = "Truststore password")
    private val truststoreType by option("--truststore-type", help = "Truststore type").default("JKS")
    private val keystoreLocation by option("--keystore-location", help = "Path to keystore (for mTLS)")
    private val keystorePassword by option("--keystore-password", help = "Keystore password")
    private val keystoreType by option("--keystore-type", help = "Keystore type").default("JKS")
    private val keyPassword by option("--key-password", help = "Key password")
    
    override fun run() {
        val saslConfig = if (saslMechanism != null) {
            SaslConfig(
                mechanism = saslMechanism!!,
                username = username,
                password = password,
                passwordFile = passwordFile,
                principal = principal,
                keytab = keytab,
                krb5Conf = krb5Conf,
                tokenEndpoint = tokenEndpoint,
                clientId = clientId,
                clientSecret = clientSecret,
                scope = scope
            )
        } else null
        
        val sslConfig = if (truststoreLocation != null || keystoreLocation != null) {
            SslConfig(
                truststoreLocation = truststoreLocation,
                truststorePassword = truststorePassword,
                truststoreType = truststoreType,
                keystoreLocation = keystoreLocation,
                keystorePassword = keystorePassword,
                keystoreType = keystoreType,
                keyPassword = keyPassword
            )
        } else null
        
        val authConfig = AuthConfigEntry(
            name = name,
            securityProtocol = securityProtocol,
            sasl = saslConfig,
            ssl = sslConfig
        )
        
        configManager.setAuthConfig(authConfig)
        output.line("Auth config \"$name\" configured with security protocol: ${securityProtocol.name}")
    }
}
