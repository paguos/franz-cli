package dev.franz.cli.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import dev.franz.cli.config.ConfigManager

class CurrentContext(
    private val configManager: ConfigManager = ConfigManager()
) : CliktCommand(
    name = "current-context",
    help = """
        Display the current context.

        Examples:
        ```
        franz config current-context
        ```
    """.trimIndent()
) {
    override fun run() {
        val currentContext = configManager.getCurrentContextName()
        
        if (currentContext == null) {
            echo("No current context set.")
            echo("Use 'franz config use-context <name>' to set one.")
        } else {
            echo(currentContext)
        }
    }
}
