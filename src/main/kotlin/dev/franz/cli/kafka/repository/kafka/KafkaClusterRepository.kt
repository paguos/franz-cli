package dev.franz.cli.kafka.repository.kafka

import dev.franz.cli.kafka.model.ClusterInfo
import dev.franz.cli.kafka.repository.ClusterRepository
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ListTopicsOptions

/**
 * Real Kafka implementation of ClusterRepository.
 * Uses Kafka AdminClient to interact with the cluster.
 */
class KafkaClusterRepository(
    private val adminClient: AdminClient
) : ClusterRepository {
    
    override fun describeCluster(): ClusterInfo {
        val clusterResult = adminClient.describeCluster()
        
        val clusterId = clusterResult.clusterId().get()
        val controller = clusterResult.controller().get()
        val nodes = clusterResult.nodes().get()
        
        // Get topic count
        val topics = adminClient.listTopics(ListTopicsOptions().listInternal(false)).names().get()
        
        // Get total partition count
        val partitionCount = if (topics.isNotEmpty()) {
            val descriptions = adminClient.describeTopics(topics).allTopicNames().get()
            descriptions.values.sumOf { it.partitions().size }
        } else {
            0
        }
        
        return ClusterInfo(
            clusterId = clusterId,
            controllerId = controller.id(),
            controllerHost = "${controller.host()}:${controller.port()}",
            brokerCount = nodes.size,
            topicCount = topics.size,
            partitionCount = partitionCount,
            isControllerActive = true,  // If we can reach the controller, it's active
            allBrokersOnline = true     // Simplified - in production, would check each broker
        )
    }
}
