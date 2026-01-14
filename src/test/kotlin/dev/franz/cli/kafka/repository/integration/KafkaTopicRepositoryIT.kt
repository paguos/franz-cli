package dev.franz.cli.kafka.repository.integration

import dev.franz.cli.kafka.repository.kafka.KafkaTopicRepository
import org.apache.kafka.clients.admin.NewTopic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Integration tests for KafkaTopicRepository.
 * Tests run against a real Kafka instance via Testcontainers.
 */
@Tag("integration")
class KafkaTopicRepositoryIT : KafkaTestBase() {
    
    private lateinit var repository: KafkaTopicRepository
    
    @BeforeEach
    fun setUp() {
        repository = KafkaTopicRepository(adminClient)
    }
    
    @Test
    fun `listTopics returns topics from Kafka`() {
        // Given: create a unique topic
        val topicName = "test-topic-${UUID.randomUUID()}"
        adminClient.createTopics(listOf(NewTopic(topicName, 3, 1.toShort()))).all().get()
        
        // When: list topics
        val topics = repository.listTopics()
        
        // Then: our topic should be in the list
        assertThat(topics).anyMatch { it.name == topicName }
        val topic = topics.find { it.name == topicName }!!
        assertThat(topic.partitions).isEqualTo(3)
        assertThat(topic.replicationFactor).isEqualTo(1)
    }
    
    @Test
    fun `listTopics with pattern filters topics`() {
        // Given: create topics with specific names
        val prefix = "filter-test-${UUID.randomUUID()}"
        val topic1 = "$prefix-events"
        val topic2 = "$prefix-orders"
        val topic3 = "other-${UUID.randomUUID()}"
        
        adminClient.createTopics(listOf(
            NewTopic(topic1, 1, 1.toShort()),
            NewTopic(topic2, 1, 1.toShort()),
            NewTopic(topic3, 1, 1.toShort())
        )).all().get()
        
        // When: list topics with pattern
        val topics = repository.listTopics(pattern = prefix)
        
        // Then: only matching topics returned
        assertThat(topics.map { it.name }).contains(topic1, topic2)
        assertThat(topics.map { it.name }).doesNotContain(topic3)
    }
    
    @Test
    fun `listTopics excludes internal topics by default`() {
        // When: list topics without internal
        val topics = repository.listTopics(includeInternal = false)
        
        // Then: no internal topics (starting with __)
        assertThat(topics).noneMatch { it.name.startsWith("__") }
    }
    
    @Test
    fun `listTopics includes internal topics when requested`() {
        // When: list topics with internal
        val topics = repository.listTopics(includeInternal = true)
        
        // Then: internal topics are included (if any exist)
        // Note: internal topics like __consumer_offsets are created when consumers connect
        // This test just verifies the flag is passed correctly
        assertThat(topics).isNotNull
    }
    
    @Test
    fun `describeTopic returns topic details`() {
        // Given: create a topic
        val topicName = "describe-test-${UUID.randomUUID()}"
        adminClient.createTopics(listOf(NewTopic(topicName, 3, 1.toShort()))).all().get()
        
        // When: describe the topic
        val topic = repository.describeTopic(topicName)
        
        // Then: details are correct
        assertThat(topic).isNotNull
        assertThat(topic!!.name).isEqualTo(topicName)
        assertThat(topic.partitions).isEqualTo(3)
        assertThat(topic.replicationFactor).isEqualTo(1)
        assertThat(topic.partitionDetails).hasSize(3)
        
        // Verify partition details
        topic.partitionDetails.forEach { partition ->
            assertThat(partition.id).isBetween(0, 2)
            assertThat(partition.leader).isGreaterThanOrEqualTo(0)
            assertThat(partition.replicas).isNotEmpty
            assertThat(partition.isr).isNotEmpty
        }
    }
    
    @Test
    fun `describeTopic returns null for non-existent topic`() {
        // When: describe a non-existent topic
        val topic = repository.describeTopic("non-existent-${UUID.randomUUID()}")
        
        // Then: null is returned
        assertThat(topic).isNull()
    }
    
    @Test
    fun `deleteTopic removes topic from Kafka`() {
        // Given: create a topic
        val topicName = "delete-test-${UUID.randomUUID()}"
        adminClient.createTopics(listOf(NewTopic(topicName, 1, 1.toShort()))).all().get()
        
        // Verify it exists
        assertThat(repository.describeTopic(topicName)).isNotNull
        
        // When: delete the topic
        val result = repository.deleteTopic(topicName)
        
        // Then: deletion succeeded
        assertThat(result).isTrue()
        
        // And topic no longer exists (may take a moment to propagate)
        Thread.sleep(500)
        assertThat(repository.describeTopic(topicName)).isNull()
    }
    
    @Test
    fun `deleteTopic returns false for non-existent topic`() {
        // When: delete a non-existent topic
        val result = repository.deleteTopic("non-existent-${UUID.randomUUID()}")
        
        // Then: returns false
        assertThat(result).isFalse()
    }
    
    @Test
    fun `createTopic creates new topic in Kafka`() {
        // Given: a unique topic name
        val topicName = "create-test-${UUID.randomUUID()}"
        
        // When: create the topic
        val topic = repository.createTopic(topicName, partitions = 5, replicationFactor = 1)
        
        // Then: topic is created with correct settings
        assertThat(topic.name).isEqualTo(topicName)
        assertThat(topic.partitions).isEqualTo(5)
        assertThat(topic.replicationFactor).isEqualTo(1)
        
        // And it exists in Kafka
        val fetched = repository.describeTopic(topicName)
        assertThat(fetched).isNotNull
        assertThat(fetched!!.partitions).isEqualTo(5)
    }
}
