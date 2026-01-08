package dev.franz.cli.kafka.repository.kafka

import dev.franz.cli.kafka.model.ConsumerGroup
import dev.franz.cli.kafka.model.GroupMember
import dev.franz.cli.kafka.model.TopicSubscription
import dev.franz.cli.kafka.repository.GroupRepository
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ListConsumerGroupsOptions
import org.apache.kafka.common.errors.GroupIdNotFoundException
import java.util.concurrent.ExecutionException

/**
 * Real Kafka implementation of GroupRepository.
 * Uses Kafka AdminClient to interact with consumer groups.
 */
class KafkaGroupRepository(
    private val adminClient: AdminClient
) : GroupRepository {
    
    override fun listGroups(includeEmpty: Boolean, pattern: String?): List<ConsumerGroup> {
        val options = ListConsumerGroupsOptions()
        val groupListings = adminClient.listConsumerGroups(options).all().get()
        
        val filteredGroups = groupListings
            .filter { pattern == null || it.groupId().contains(pattern, ignoreCase = true) }
            .mapNotNull { listing ->
                val group = describeGroup(listing.groupId())
                if (includeEmpty || group?.state != "Empty") group else null
            }
        
        return filteredGroups
    }
    
    override fun describeGroup(name: String): ConsumerGroup? {
        return try {
            val descriptions = adminClient.describeConsumerGroups(listOf(name)).all().get()
            val description = descriptions[name] ?: return null
            
            val members = description.members().map { member ->
                val assignments = member.assignment().topicPartitions().map { tp ->
                    "${tp.topic()}[${tp.partition()}]"
                }
                GroupMember(
                    memberId = member.consumerId(),
                    clientId = member.clientId(),
                    assignments = assignments
                )
            }
            
            // Get topic subscriptions from member assignments
            val topicPartitions = description.members()
                .flatMap { it.assignment().topicPartitions() }
                .groupBy { it.topic() }
                .map { (topic, partitions) -> TopicSubscription(topic, partitions.size) }
            
            val coordinator = description.coordinator()
            val coordinatorStr = if (coordinator != null && coordinator.host() != null) {
                "${coordinator.host()}:${coordinator.port()} (id: ${coordinator.id()})"
            } else {
                ""
            }
            
            ConsumerGroup(
                name = description.groupId(),
                state = description.state().toString(),
                protocolType = description.partitionAssignor(),
                protocol = description.partitionAssignor(),
                coordinator = coordinatorStr,
                topicSubscriptions = topicPartitions,
                totalLag = 0, // Lag calculation requires offset fetching
                members = members
            )
        } catch (e: ExecutionException) {
            if (e.cause is GroupIdNotFoundException) {
                null
            } else {
                throw e
            }
        }
    }
    
    override fun deleteGroup(name: String): Boolean {
        return try {
            adminClient.deleteConsumerGroups(listOf(name)).all().get()
            true
        } catch (e: ExecutionException) {
            if (e.cause is GroupIdNotFoundException) {
                false
            } else {
                throw e
            }
        }
    }
}
