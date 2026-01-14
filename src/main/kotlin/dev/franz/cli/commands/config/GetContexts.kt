package dev.franz.cli.commands.config

import dev.franz.cli.FranzCommand
import dev.franz.cli.config.ConfigManager

class GetContexts(
    private val configManager: ConfigManager = ConfigManager()
) : FranzCommand(
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
            output.line("No contexts configured.")
            output.line("Use 'franz config set-context <name>' to create one.")
            return
        }

        output.table(
            headers = listOf("CURRENT", "NAME", "CLUSTER", "AUTH"),
            rows = config.contexts.map { context ->
                val current = if (context.name == config.currentContext) "*" else ""
                val auth = context.auth ?: "-"
                listOf(current, context.name, context.cluster, auth)
            }
        )
    }
}
