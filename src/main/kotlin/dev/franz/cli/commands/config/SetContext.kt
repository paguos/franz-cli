package dev.franz.cli.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.franz.cli.config.ConfigManager
import dev.franz.cli.config.model.ContextEntry

class SetContext(
    private val configManager: ConfigManager = ConfigManager()
) : CliktCommand(
    name = "set-context",
    help = """
        Create or update a context (ties a name to a cluster + optional auth config).

        Examples:
        ```
        franz config set-context local --cluster local
        franz config set-context prod --cluster prod --auth prod-sasl
        ```
    """.trimIndent()
) {
    private val name by argument(help = "Name of the context")
    private val cluster by option("--cluster", "-c", help = "Cluster name to reference").required()
    private val auth by option("--auth", "-a", help = "Auth config name to reference")
    
    override fun run() {
        val context = ContextEntry(
            name = name,
            cluster = cluster,
            auth = auth
        )
        
        configManager.setContext(context)
        echo("Context \"$name\" configured.")
    }
}
