package dev.franz.cli.commands.config

import dev.franz.cli.FranzCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.franz.cli.config.ConfigManager

class UseContext(
    private val configManager: ConfigManager = ConfigManager()
) : FranzCommand(
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
        output.line("Switched to context \"$name\".")
    }
}
