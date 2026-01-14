package dev.franz.cli.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.franz.cli.config.ConfigManager

class UseContext(
    private val configManager: ConfigManager = ConfigManager()
) : CliktCommand(
    name = "use-context",
    help = """
        Set the current context.

        Examples:
        ```
        franz config use-context local
        ```
    """.trimIndent()
) {
    private val name by argument(help = "Name of the context to use")
    
    override fun run() {
        configManager.setCurrentContext(name)
        echo("Switched to context \"$name\".")
    }
}
