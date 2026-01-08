package dev.franz.cli.commands.resources

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.franz.cli.kafka.KafkaService

class GetTopic : CliktCommand(
    name = "topic",
    help = "List Kafka topics"
) {
    private val pattern by argument(help = "Optional topic name pattern to filter").optional()
    private val showInternal by option("--show-internal", "-i", help = "Include internal topics").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        echo("Listing topics...")
        if (pattern != null) {
            echo("  Filter pattern: $pattern")
        }
        if (showInternal) {
            echo("  Including internal topics")
        }
        echo()
        
        val topics = kafka.topics.listTopics(includeInternal = showInternal, pattern = pattern)
        
        echo("TOPIC                    PARTITIONS   REPLICAS")
        topics.forEach { topic ->
            echo("${topic.name.padEnd(24)} ${topic.partitions.toString().padEnd(12)} ${topic.replicationFactor}")
        }
    }
}

class DescribeTopic : CliktCommand(
    name = "topic",
    help = "Show detailed information about a topic"
) {
    private val name by argument(help = "Topic name")

    override fun run() {
        val kafka = KafkaService.getInstance()
        val topic = kafka.topics.describeTopic(name)
        
        if (topic == null) {
            echo("Topic '$name' not found.", err = true)
            return
        }
        
        echo("Topic: ${topic.name}")
        echo("=".repeat(50))
        echo("Partitions:        ${topic.partitions}")
        echo("Replication:       ${topic.replicationFactor}")
        echo("Cleanup Policy:    ${topic.cleanupPolicy}")
        echo("Retention (ms):    ${topic.retentionMs} (${topic.retentionMs / 86400000} days)")
        echo()
        
        if (topic.partitionDetails.isNotEmpty()) {
            echo("Partition Details:")
            topic.partitionDetails.forEach { p ->
                echo("  Partition ${p.id}: Leader=${p.leader}, Replicas=${p.replicas}, ISR=${p.isr}")
            }
        }
    }
}

class DeleteTopic : CliktCommand(
    name = "topic",
    help = "Delete a Kafka topic"
) {
    private val name by argument(help = "Topic name to delete")
    private val force by option("--force", "-f", help = "Skip confirmation").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        if (force) {
            val deleted = kafka.topics.deleteTopic(name)
            if (deleted) {
                echo("Deleted topic '$name'.")
            } else {
                echo("Topic '$name' not found.", err = true)
            }
        } else {
            echo("Would delete topic '$name'. Use --force to confirm.")
        }
    }
}
