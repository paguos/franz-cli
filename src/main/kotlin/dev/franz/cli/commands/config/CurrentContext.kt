package dev.franz.cli.commands.config

import dev.franz.cli.FranzCommand
import dev.franz.cli.config.ConfigManager

class CurrentContext(
    private val configManager: ConfigManager = ConfigManager()
) : FranzCommand(
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
            output.line("No current context set.")
            output.line("Use 'franz config use-context <name>' to set one.")
        } else {
            output.line(currentContext)
        }
    }
}
