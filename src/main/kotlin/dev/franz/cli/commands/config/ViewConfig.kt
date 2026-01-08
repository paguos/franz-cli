package dev.franz.cli.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import dev.franz.cli.config.ConfigManager

class ViewConfig(
    private val configManager: ConfigManager = ConfigManager()
) : CliktCommand(
    name = "view",
    help = "Display the current configuration (secrets are redacted)"
) {
    override fun run() {
        val config = configManager.loadConfig()
        
        echo("apiVersion: ${config.apiVersion}")
        echo("current-context: ${config.currentContext ?: "(none)"}")
        echo()
        
        if (config.contexts.isNotEmpty()) {
            echo("contexts:")
            config.contexts.forEach { ctx ->
                echo("  - name: ${ctx.name}")
                echo("    cluster: ${ctx.cluster}")
                ctx.auth?.let { echo("    auth: $it") }
            }
            echo()
        }
        
        if (config.clusters.isNotEmpty()) {
            echo("clusters:")
            config.clusters.forEach { cluster ->
                echo("  - name: ${cluster.name}")
                echo("    bootstrap-servers: ${cluster.bootstrapServers}")
            }
            echo()
        }
        
        if (config.authConfigs.isNotEmpty()) {
            echo("auth-configs:")
            config.authConfigs.forEach { auth ->
                echo("  - name: ${auth.name}")
                echo("    security-protocol: ${auth.securityProtocol}")
                
                auth.sasl?.let { sasl ->
                    echo("    sasl:")
                    echo("      mechanism: ${sasl.mechanism}")
                    sasl.username?.let { echo("      username: $it") }
                    if (sasl.password != null || sasl.passwordFile != null) {
                        echo("      password: ***")
                    }
                    sasl.principal?.let { echo("      principal: $it") }
                    sasl.keytab?.let { echo("      keytab: $it") }
                    sasl.tokenEndpoint?.let { echo("      token-endpoint: $it") }
                    sasl.clientId?.let { echo("      client-id: $it") }
                    if (sasl.clientSecret != null) {
                        echo("      client-secret: ***")
                    }
                }
                
                auth.ssl?.let { ssl ->
                    echo("    ssl:")
                    ssl.truststoreLocation?.let { echo("      truststore-location: $it") }
                    if (ssl.truststorePassword != null) {
                        echo("      truststore-password: ***")
                    }
                    ssl.keystoreLocation?.let { echo("      keystore-location: $it") }
                    if (ssl.keystorePassword != null) {
                        echo("      keystore-password: ***")
                    }
                }
            }
        }
    }
}
