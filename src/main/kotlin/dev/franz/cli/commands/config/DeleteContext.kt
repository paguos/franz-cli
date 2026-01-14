package dev.franz.cli.commands.config

import dev.franz.cli.FranzCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.franz.cli.config.ConfigManager

class DeleteContext(
    private val configManager: ConfigManager = ConfigManager()
) : FranzCommand(
    name = "delete-context",
    help = """
        Delete a context.

        Examples:
        ```
        franz config delete-context local
        ```
    """.trimIndent()
) {
    private val name by argument(help = "Name of the context to delete")
    
    override fun run() {
        val deleted = configManager.deleteContext(name)
        
        if (deleted) {
            output.line("Context \"$name\" deleted.")
        } else {
            output.line("Context \"$name\" not found.")
        }
    }
}
