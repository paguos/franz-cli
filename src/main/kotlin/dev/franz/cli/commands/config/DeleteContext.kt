package dev.franz.cli.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.franz.cli.config.ConfigManager

class DeleteContext(
    private val configManager: ConfigManager = ConfigManager()
) : CliktCommand(
    name = "delete-context",
    help = "Delete a context"
) {
    private val name by argument(help = "Name of the context to delete")
    
    override fun run() {
        val deleted = configManager.deleteContext(name)
        
        if (deleted) {
            echo("Context \"$name\" deleted.")
        } else {
            echo("Context \"$name\" not found.")
        }
    }
}
