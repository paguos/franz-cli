package dev.franz.cli.commands.config

import dev.franz.cli.FranzCommand
import dev.franz.cli.config.ConfigManager

class ViewConfig(
    private val configManager: ConfigManager = ConfigManager()
) : FranzCommand(
    name = "view",
    help = """
        Display the current configuration (secrets are redacted).

        Examples:
        ```
        franz config view
        ```
    """.trimIndent()
) {
    override fun run() {
        val config = configManager.loadConfig()

        output.line("apiVersion: ${config.apiVersion}")
        output.line("current-context: ${config.currentContext ?: "(none)"}")
        output.line()

        if (config.contexts.isNotEmpty()) {
            output.line("contexts:")
            config.contexts.forEach { ctx ->
                output.line("  - name: ${ctx.name}")
                output.line("    cluster: ${ctx.cluster}")
                ctx.auth?.let { output.line("    auth: $it") }
            }
            output.line()
        }
        
        if (config.clusters.isNotEmpty()) {
            output.line("clusters:")
            config.clusters.forEach { cluster ->
                output.line("  - name: ${cluster.name}")
                output.line("    bootstrap-servers: ${cluster.bootstrapServers}")
            }
            output.line()
        }
        
        if (config.authConfigs.isNotEmpty()) {
            output.line("auth-configs:")
            config.authConfigs.forEach { auth ->
                output.line("  - name: ${auth.name}")
                output.line("    security-protocol: ${auth.securityProtocol}")
                
                auth.sasl?.let { sasl ->
                    output.line("    sasl:")
                    output.line("      mechanism: ${sasl.mechanism}")
                    sasl.username?.let { output.line("      username: $it") }
                    if (sasl.password != null || sasl.passwordFile != null) {
                        output.line("      password: ***")
                    }
                    sasl.principal?.let { output.line("      principal: $it") }
                    sasl.keytab?.let { output.line("      keytab: $it") }
                    sasl.tokenEndpoint?.let { output.line("      token-endpoint: $it") }
                    sasl.clientId?.let { output.line("      client-id: $it") }
                    if (sasl.clientSecret != null) {
                        output.line("      client-secret: ***")
                    }
                }
                
                auth.ssl?.let { ssl ->
                    output.line("    ssl:")
                    ssl.truststoreLocation?.let { output.line("      truststore-location: $it") }
                    if (ssl.truststorePassword != null) {
                        output.line("      truststore-password: ***")
                    }
                    ssl.keystoreLocation?.let { output.line("      keystore-location: $it") }
                    if (ssl.keystorePassword != null) {
                        output.line("      keystore-password: ***")
                    }
                }
            }
        }
    }
}
