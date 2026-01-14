package dev.franz.cli.commands.resources

import dev.franz.cli.FranzCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.franz.cli.kafka.KafkaService

class GetTopic : FranzCommand(
    name = "topic",
    help = """
        List Kafka topics.

        Examples:
        ```
        franz get topic
        franz get topic payments-*
        franz get topic --show-internal
        ```
    """.trimIndent()
) {
    private val pattern by argument(help = "Optional topic name pattern to filter").optional()
    private val showInternal by option("--show-internal", "-i", help = "Include internal topics").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        val topics = kafka.topics.listTopics(includeInternal = showInternal, pattern = pattern)

        output.table(
            headers = listOf("TOPIC", "PARTITIONS", "REPLICAS"),
            rows = topics.map { topic ->
                listOf(
                    topic.name,
                    topic.partitions.toString(),
                    topic.replicationFactor.toString()
                )
            }
        )
    }
}

class DescribeTopic : FranzCommand(
    name = "topic",
    help = """
        Show detailed information about a topic.

        Examples:
        ```
        franz describe topic my-topic
        ```
    """.trimIndent()
) {
    private val name by argument(help = "Topic name")

    override fun run() {
        val kafka = KafkaService.getInstance()
        val topic = kafka.topics.describeTopic(name)
        
        if (topic == null) {
            errorLine("Topic '$name' not found.")
            return
        }

        output.kvTable(
            listOf(
                "Name" to topic.name,
                "Partitions" to topic.partitions.toString(),
                "Replication" to topic.replicationFactor.toString(),
                "Cleanup Policy" to topic.cleanupPolicy,
                "Retention (ms)" to "${topic.retentionMs} (${topic.retentionMs / 86400000} days)"
            )
        )

        if (topic.partitionDetails.isNotEmpty()) {
            output.line()
            output.section("Partition Details")
            output.table(
                headers = listOf("ID", "LEADER", "REPLICAS", "ISR"),
                rows = topic.partitionDetails.map { partition ->
                    listOf(
                        partition.id.toString(),
                        partition.leader.toString(),
                        partition.replicas.joinToString(","),
                        partition.isr.joinToString(",")
                    )
                }
            )
        }
    }
}

class DeleteTopic : FranzCommand(
    name = "topic",
    help = """
        Delete a Kafka topic.

        Examples:
        ```
        franz delete topic my-topic --force
        ```
    """.trimIndent()
) {
    private val name by argument(help = "Topic name to delete")
    private val force by option("--force", "-f", help = "Skip confirmation").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        if (force) {
            val deleted = kafka.topics.deleteTopic(name)
            if (deleted) {
                output.line("Deleted topic '$name'.")
            } else {
                errorLine("Topic '$name' not found.")
            }
        } else {
            output.line("Would delete topic '$name'. Use --force to confirm.")
        }
    }
}
