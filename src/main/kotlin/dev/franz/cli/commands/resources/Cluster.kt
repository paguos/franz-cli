package dev.franz.cli.commands.resources

import dev.franz.cli.FranzCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.franz.cli.kafka.KafkaService

class DescribeCluster : FranzCommand(
    name = "cluster",
    help = """
        Show Kafka cluster information.

        Examples:
        ```
        franz describe cluster
        franz describe cluster --health
        franz describe cluster --topics
        ```
    """.trimIndent()
) {
    private val showTopics by option("--topics", "-t", help = "Include topic summary").flag()
    private val showHealth by option("--health", "-H", help = "Show cluster health status").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        val cluster = kafka.cluster.describeCluster()

        output.kvTable(
            listOf(
                "Cluster ID" to cluster.clusterId,
                "Controller" to "${cluster.controllerHost} (id: ${cluster.controllerId})",
                "Brokers" to cluster.brokerCount.toString(),
                "Topics" to cluster.topicCount.toString(),
                "Partitions" to cluster.partitionCount.toString(),
                "Kafka Version" to cluster.kafkaVersion,
                "Protocol Version" to cluster.protocolVersion
            )
        )

        if (showHealth) {
            val isHealthy = cluster.underReplicatedPartitions == 0 && 
                           cluster.offlinePartitions == 0 && 
                           cluster.isControllerActive && 
                           cluster.allBrokersOnline
            output.line()
            output.section("Health Status")
            output.kvTable(
                listOf(
                    "Under-replicated Partitions" to cluster.underReplicatedPartitions.toString(),
                    "Offline Partitions" to cluster.offlinePartitions.toString(),
                    "Controller Active" to if (cluster.isControllerActive) "Yes" else "No",
                    "All Brokers Online" to if (cluster.allBrokersOnline) "Yes" else "No",
                    "Status" to if (isHealthy) "HEALTHY" else "DEGRADED"
                )
            )
        }
        
        if (showTopics) {
            val topics = kafka.topics.listTopics(includeInternal = true)
            val internalCount = topics.count { it.isInternal }
            val userCount = topics.size - internalCount

            output.line()
            output.section("Topic Summary")
            output.kvTable(
                listOf(
                    "Internal Topics" to internalCount.toString(),
                    "User Topics" to userCount.toString()
                )
            )
        }
    }
}
