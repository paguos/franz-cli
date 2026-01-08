package dev.franz.cli.kafka.repository.mock

import dev.franz.cli.kafka.model.Partition
import dev.franz.cli.kafka.model.Topic
import dev.franz.cli.kafka.repository.TopicRepository

class MockTopicRepository : TopicRepository {
    
    private val topics = mutableListOf(
        Topic(
            name = "my-topic",
            partitions = 3,
            replicationFactor = 2,
            partitionDetails = listOf(
                Partition(0, 1, listOf(1, 2), listOf(1, 2)),
                Partition(1, 2, listOf(2, 3), listOf(2, 3)),
                Partition(2, 3, listOf(3, 1), listOf(3, 1))
            )
        ),
        Topic(
            name = "another-topic",
            partitions = 6,
            replicationFactor = 3
        ),
        Topic(
            name = "events",
            partitions = 12,
            replicationFactor = 2
        ),
        Topic(
            name = "__consumer_offsets",
            partitions = 50,
            replicationFactor = 3,
            isInternal = true,
            cleanupPolicy = "compact"
        )
    )
    
    override fun listTopics(includeInternal: Boolean, pattern: String?): List<Topic> {
        return topics
            .filter { includeInternal || !it.isInternal }
            .filter { pattern == null || it.name.contains(pattern, ignoreCase = true) }
    }
    
    override fun describeTopic(name: String): Topic? {
        return topics.find { it.name == name }
    }
    
    override fun deleteTopic(name: String): Boolean {
        return topics.removeIf { it.name == name }
    }
    
    override fun createTopic(name: String, partitions: Int, replicationFactor: Int): Topic {
        val topic = Topic(name = name, partitions = partitions, replicationFactor = replicationFactor)
        topics.add(topic)
        return topic
    }
}
