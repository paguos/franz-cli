package dev.franz.cli.kafka.repository.kafka

import dev.franz.cli.kafka.model.Partition
import dev.franz.cli.kafka.model.Topic
import dev.franz.cli.kafka.repository.TopicRepository
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ListTopicsOptions
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import java.util.concurrent.ExecutionException

/**
 * Real Kafka implementation of TopicRepository.
 * Uses Kafka AdminClient to interact with the cluster.
 */
class KafkaTopicRepository(
    private val adminClient: AdminClient
) : TopicRepository {
    
    override fun listTopics(includeInternal: Boolean, pattern: String?): List<Topic> {
        val options = ListTopicsOptions().listInternal(includeInternal)
        val topicNames = adminClient.listTopics(options).names().get()
        
        val filteredNames = if (pattern != null) {
            topicNames.filter { it.contains(pattern, ignoreCase = true) }
        } else {
            topicNames.toList()
        }
        
        return filteredNames.mapNotNull { name -> describeTopic(name) }
    }
    
    override fun describeTopic(name: String): Topic? {
        return try {
            val descriptions = adminClient.describeTopics(listOf(name)).allTopicNames().get()
            val description = descriptions[name] ?: return null
            
            val partitionDetails = description.partitions().map { partition ->
                Partition(
                    id = partition.partition(),
                    leader = partition.leader()?.id() ?: -1,
                    replicas = partition.replicas().map { it.id() },
                    isr = partition.isr().map { it.id() }
                )
            }
            
            Topic(
                name = description.name(),
                partitions = description.partitions().size,
                replicationFactor = description.partitions().firstOrNull()?.replicas()?.size ?: 0,
                isInternal = description.isInternal,
                partitionDetails = partitionDetails
            )
        } catch (e: ExecutionException) {
            if (e.cause is UnknownTopicOrPartitionException) {
                null
            } else {
                throw e
            }
        }
    }
    
    override fun deleteTopic(name: String): Boolean {
        return try {
            // Check if topic exists first
            val exists = adminClient.listTopics().names().get().contains(name)
            if (!exists) {
                return false
            }
            
            adminClient.deleteTopics(listOf(name)).all().get()
            true
        } catch (e: ExecutionException) {
            if (e.cause is UnknownTopicOrPartitionException) {
                false
            } else {
                throw e
            }
        }
    }
    
    override fun createTopic(name: String, partitions: Int, replicationFactor: Int): Topic {
        val newTopic = NewTopic(name, partitions, replicationFactor.toShort())
        adminClient.createTopics(listOf(newTopic)).all().get()
        
        // Fetch and return the created topic
        return describeTopic(name) ?: throw IllegalStateException("Topic was created but could not be described")
    }
}
