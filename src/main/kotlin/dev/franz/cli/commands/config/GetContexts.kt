package dev.franz.cli.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import dev.franz.cli.config.ConfigManager

class GetContexts(
    private val configManager: ConfigManager = ConfigManager()
) : CliktCommand(
    name = "get-contexts",
    help = """
        List all configured contexts.

        Examples:
        ```
        franz config get-contexts
        ```
    """.trimIndent()
) {
    override fun run() {
        val config = configManager.loadConfig()
        
        if (config.contexts.isEmpty()) {
            echo("No contexts configured.")
            echo("Use 'franz config set-context <name>' to create one.")
            return
        }
        
        echo("CURRENT   NAME                 CLUSTER              AUTH")
        config.contexts.forEach { context ->
            val current = if (context.name == config.currentContext) "*" else " "
            val auth = context.auth ?: "-"
            echo("$current         ${context.name.padEnd(20)} ${context.cluster.padEnd(20)} $auth")
        }
    }
}
