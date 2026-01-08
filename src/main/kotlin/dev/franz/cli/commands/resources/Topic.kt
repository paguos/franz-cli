package dev.franz.cli.commands.resources

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class GetTopic : CliktCommand(
    name = "topic",
    help = "List Kafka topics"
) {
    private val pattern by argument(help = "Optional topic name pattern to filter").optional()
    private val showInternal by option("--show-internal", "-i", help = "Include internal topics").flag()

    override fun run() {
        echo("Listing topics...")
        if (pattern != null) {
            echo("  Filter pattern: $pattern")
        }
        if (showInternal) {
            echo("  Including internal topics")
        }
        echo()
        echo("TOPIC                    PARTITIONS   REPLICAS")
        echo("my-topic                 3            2")
        echo("another-topic            6            3")
        echo("events                   12           2")
        if (showInternal) {
            echo("__consumer_offsets       50           3")
        }
    }
}

class DescribeTopic : CliktCommand(
    name = "topic",
    help = "Show detailed information about a topic"
) {
    private val name by argument(help = "Topic name")

    override fun run() {
        echo("Topic: $name")
        echo("=" .repeat(50))
        echo("Partitions:        3")
        echo("Replication:       2")
        echo("Cleanup Policy:    delete")
        echo("Retention (ms):    604800000 (7 days)")
        echo()
        echo("Partition Details:")
        echo("  Partition 0: Leader=1, Replicas=[1,2], ISR=[1,2]")
        echo("  Partition 1: Leader=2, Replicas=[2,3], ISR=[2,3]")
        echo("  Partition 2: Leader=3, Replicas=[3,1], ISR=[3,1]")
    }
}

class DeleteTopic : CliktCommand(
    name = "topic",
    help = "Delete a Kafka topic"
) {
    private val name by argument(help = "Topic name to delete")
    private val force by option("--force", "-f", help = "Skip confirmation").flag()

    override fun run() {
        if (force) {
            echo("Deleting topic '$name'...")
        } else {
            echo("Would delete topic '$name'. Use --force to confirm.")
        }
    }
}
