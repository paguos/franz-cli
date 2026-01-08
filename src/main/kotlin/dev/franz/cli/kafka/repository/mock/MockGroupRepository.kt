package dev.franz.cli.kafka.repository.mock

import dev.franz.cli.kafka.model.ConsumerGroup
import dev.franz.cli.kafka.model.GroupMember
import dev.franz.cli.kafka.model.TopicSubscription
import dev.franz.cli.kafka.repository.GroupRepository

class MockGroupRepository : GroupRepository {
    
    private val groups = mutableListOf(
        ConsumerGroup(
            name = "my-consumer-group",
            state = "Stable",
            protocolType = "consumer",
            protocol = "range",
            coordinator = "broker-1.kafka:9092 (id: 1)",
            topicSubscriptions = listOf(
                TopicSubscription("my-topic", 3),
                TopicSubscription("events", 12)
            ),
            totalLag = 1234,
            members = listOf(
                GroupMember("consumer-1", "app-1", listOf("my-topic[0,1]", "events[0,1,2,3]")),
                GroupMember("consumer-2", "app-2", listOf("my-topic[2]", "events[4,5,6,7]")),
                GroupMember("consumer-3", "app-3", listOf("events[8,9,10,11]"))
            )
        ),
        ConsumerGroup(
            name = "analytics-consumers",
            state = "Stable",
            members = listOf(
                GroupMember("analytics-1", "analytics-app", emptyList()),
                GroupMember("analytics-2", "analytics-app", emptyList()),
                GroupMember("analytics-3", "analytics-app", emptyList()),
                GroupMember("analytics-4", "analytics-app", emptyList()),
                GroupMember("analytics-5", "analytics-app", emptyList())
            )
        ),
        ConsumerGroup(
            name = "batch-processor",
            state = "Empty",
            members = emptyList()
        )
    )
    
    override fun listGroups(includeEmpty: Boolean, pattern: String?): List<ConsumerGroup> {
        return groups
            .filter { includeEmpty || it.state != "Empty" }
            .filter { pattern == null || it.name.contains(pattern, ignoreCase = true) }
    }
    
    override fun describeGroup(name: String): ConsumerGroup? {
        return groups.find { it.name == name }
    }
    
    override fun deleteGroup(name: String): Boolean {
        return groups.removeIf { it.name == name }
    }
}
