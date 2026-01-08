package dev.franz.cli.kafka.repository

import dev.franz.cli.kafka.model.Topic

interface TopicRepository {
    fun listTopics(includeInternal: Boolean = false, pattern: String? = null): List<Topic>
    fun describeTopic(name: String): Topic?
    fun deleteTopic(name: String): Boolean
    fun createTopic(name: String, partitions: Int, replicationFactor: Int): Topic
}
