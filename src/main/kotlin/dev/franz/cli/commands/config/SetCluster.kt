package dev.franz.cli.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.franz.cli.config.ConfigManager
import dev.franz.cli.config.model.ClusterEntry

class SetCluster(
    private val configManager: ConfigManager = ConfigManager()
) : CliktCommand(
    name = "set-cluster",
    help = """
        Create or update a cluster configuration.

        Examples:
        ```
        franz config set-cluster local -b localhost:9092
        franz config set-cluster prod -b broker1:9092,broker2:9092
        ```
    """.trimIndent()
) {
    private val name by argument(help = "Name of the cluster")
    private val bootstrapServers by option(
        "--bootstrap-servers", "-b",
        help = "Kafka bootstrap servers (e.g., localhost:9092)"
    ).required()
    
    override fun run() {
        val cluster = ClusterEntry(
            name = name,
            bootstrapServers = bootstrapServers
        )
        
        configManager.setCluster(cluster)
        echo("Cluster \"$name\" configured with bootstrap servers: $bootstrapServers")
    }
}
