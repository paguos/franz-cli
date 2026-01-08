package dev.franz.cli.commands.resources

import dev.franz.cli.kafka.KafkaService
import dev.franz.cli.kafka.model.Partition
import dev.franz.cli.kafka.model.Topic
import dev.franz.cli.kafka.repository.TopicRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class TopicCommandsTest {
    
    private lateinit var topicRepository: TopicRepository
    private lateinit var kafkaService: KafkaService
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var errorStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream
    
    @BeforeEach
    fun setUp() {
        topicRepository = mockk()
        kafkaService = KafkaService(topics = topicRepository)
        KafkaService.setInstance(kafkaService)
        
        outputStream = ByteArrayOutputStream()
        errorStream = ByteArrayOutputStream()
        originalOut = System.out
        originalErr = System.err
        System.setOut(PrintStream(outputStream))
        System.setErr(PrintStream(errorStream))
    }
    
    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
        System.setErr(originalErr)
        KafkaService.resetInstance()
    }
    
    private fun getAllOutput(): String = outputStream.toString() + errorStream.toString()
    
    @Test
    fun `GetTopic lists all topics`() {
        val topics = listOf(
            Topic("test-topic-1", 3, 2),
            Topic("test-topic-2", 6, 3)
        )
        every { topicRepository.listTopics(includeInternal = false, pattern = null) } returns topics
        
        GetTopic(kafkaService).main(emptyArray())
        
        val output = outputStream.toString()
        assertThat(output).contains("test-topic-1")
        assertThat(output).contains("test-topic-2")
        assertThat(output).contains("TOPIC")
        assertThat(output).contains("PARTITIONS")
        verify { topicRepository.listTopics(includeInternal = false, pattern = null) }
    }
    
    @Test
    fun `GetTopic with pattern filters topics`() {
        val topics = listOf(Topic("events", 12, 2))
        every { topicRepository.listTopics(includeInternal = false, pattern = "events") } returns topics
        
        GetTopic(kafkaService).main(arrayOf("events"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Filter pattern: events")
        assertThat(output).contains("events")
        verify { topicRepository.listTopics(includeInternal = false, pattern = "events") }
    }
    
    @Test
    fun `GetTopic with show-internal includes internal topics`() {
        val topics = listOf(
            Topic("test-topic", 3, 2),
            Topic("__consumer_offsets", 50, 3, isInternal = true)
        )
        every { topicRepository.listTopics(includeInternal = true, pattern = null) } returns topics
        
        GetTopic(kafkaService).main(arrayOf("--show-internal"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Including internal topics")
        assertThat(output).contains("__consumer_offsets")
        verify { topicRepository.listTopics(includeInternal = true, pattern = null) }
    }
    
    @Test
    fun `DescribeTopic shows topic details`() {
        val topic = Topic(
            name = "my-topic",
            partitions = 3,
            replicationFactor = 2,
            cleanupPolicy = "delete",
            retentionMs = 604800000,
            partitionDetails = listOf(
                Partition(0, 1, listOf(1, 2), listOf(1, 2)),
                Partition(1, 2, listOf(2, 3), listOf(2, 3))
            )
        )
        every { topicRepository.describeTopic("my-topic") } returns topic
        
        DescribeTopic(kafkaService).main(arrayOf("my-topic"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Topic: my-topic")
        assertThat(output).contains("Partitions:        3")
        assertThat(output).contains("Replication:       2")
        assertThat(output).contains("Cleanup Policy:    delete")
        assertThat(output).contains("Partition 0: Leader=1")
        verify { topicRepository.describeTopic("my-topic") }
    }
    
    @Test
    fun `DescribeTopic shows error for non-existent topic`() {
        every { topicRepository.describeTopic("non-existent") } returns null
        
        DescribeTopic(kafkaService).main(arrayOf("non-existent"))
        
        val output = getAllOutput()
        assertThat(output).contains("not found")
    }
    
    @Test
    fun `DeleteTopic without force shows confirmation message`() {
        DeleteTopic(kafkaService).main(arrayOf("my-topic"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Would delete topic 'my-topic'")
        assertThat(output).contains("Use --force to confirm")
    }
    
    @Test
    fun `DeleteTopic with force deletes topic`() {
        every { topicRepository.deleteTopic("my-topic") } returns true
        
        DeleteTopic(kafkaService).main(arrayOf("my-topic", "--force"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Deleted topic 'my-topic'")
        verify { topicRepository.deleteTopic("my-topic") }
    }
}
