package dev.franz.cli.commands.resources

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.franz.cli.kafka.KafkaService

class DescribeCluster(
    private val kafka: KafkaService = KafkaService.getInstance()
) : CliktCommand(
    name = "cluster",
    help = "Show Kafka cluster information"
) {
    private val showTopics by option("--topics", "-t", help = "Include topic summary").flag()
    private val showHealth by option("--health", "-H", help = "Show cluster health status").flag()

    override fun run() {
        val cluster = kafka.cluster.describeCluster()
        
        echo("Cluster Information")
        echo("=".repeat(50))
        echo("Cluster ID:        ${cluster.clusterId}")
        echo("Controller:        ${cluster.controllerHost} (id: ${cluster.controllerId})")
        echo("Brokers:           ${cluster.brokerCount}")
        echo("Topics:            ${cluster.topicCount}")
        echo("Partitions:        ${cluster.partitionCount}")
        echo()
        echo("Kafka Version:     ${cluster.kafkaVersion}")
        echo("Protocol Version:  ${cluster.protocolVersion}")
        
        if (showHealth) {
            echo()
            echo("Health Status:")
            echo("  Under-replicated Partitions: ${cluster.underReplicatedPartitions}")
            echo("  Offline Partitions:          ${cluster.offlinePartitions}")
            echo("  Controller Active:           ${if (cluster.isControllerActive) "Yes" else "No"}")
            echo("  All Brokers Online:          ${if (cluster.allBrokersOnline) "Yes" else "No"}")
            echo()
            val isHealthy = cluster.underReplicatedPartitions == 0 && 
                           cluster.offlinePartitions == 0 && 
                           cluster.isControllerActive && 
                           cluster.allBrokersOnline
            echo("Status: ${if (isHealthy) "HEALTHY" else "DEGRADED"}")
        }
        
        if (showTopics) {
            val topics = kafka.topics.listTopics(includeInternal = true)
            val internalCount = topics.count { it.isInternal }
            val userCount = topics.size - internalCount
            
            echo()
            echo("Topic Summary:")
            echo("  Internal Topics:    $internalCount")
            echo("  User Topics:        $userCount")
        }
    }
}
