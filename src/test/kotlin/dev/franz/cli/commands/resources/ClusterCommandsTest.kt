package dev.franz.cli.commands.resources

import dev.franz.cli.kafka.KafkaService
import dev.franz.cli.kafka.model.ClusterInfo
import dev.franz.cli.kafka.model.Topic
import dev.franz.cli.kafka.repository.ClusterRepository
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

class ClusterCommandsTest {
    
    private lateinit var clusterRepository: ClusterRepository
    private lateinit var topicRepository: TopicRepository
    private lateinit var kafkaService: KafkaService
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    
    @BeforeEach
    fun setUp() {
        clusterRepository = mockk()
        topicRepository = mockk()
        kafkaService = KafkaService(cluster = clusterRepository, topics = topicRepository)
        KafkaService.setInstance(kafkaService)
        
        outputStream = ByteArrayOutputStream()
        originalOut = System.out
        System.setOut(PrintStream(outputStream))
    }
    
    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
        KafkaService.resetInstance()
    }
    
    @Test
    fun `DescribeCluster shows cluster information`() {
        val cluster = ClusterInfo(
            clusterId = "abc123-def456",
            controllerId = 1,
            controllerHost = "broker-1.kafka:9092",
            brokerCount = 3,
            topicCount = 42,
            partitionCount = 156,
            kafkaVersion = "3.6.0",
            protocolVersion = "3.6"
        )
        every { clusterRepository.describeCluster() } returns cluster
        
        DescribeCluster(kafkaService).main(emptyArray())
        
        val output = outputStream.toString()
        assertThat(output).contains("Cluster Information")
        assertThat(output).contains("Cluster ID:        abc123-def456")
        assertThat(output).contains("Controller:        broker-1.kafka:9092 (id: 1)")
        assertThat(output).contains("Brokers:           3")
        assertThat(output).contains("Topics:            42")
        assertThat(output).contains("Kafka Version:     3.6.0")
        verify { clusterRepository.describeCluster() }
    }
    
    @Test
    fun `DescribeCluster with health shows health status`() {
        val cluster = ClusterInfo(
            clusterId = "abc123",
            controllerId = 1,
            controllerHost = "broker-1.kafka:9092",
            brokerCount = 3,
            topicCount = 10,
            partitionCount = 30,
            underReplicatedPartitions = 0,
            offlinePartitions = 0,
            isControllerActive = true,
            allBrokersOnline = true
        )
        every { clusterRepository.describeCluster() } returns cluster
        
        DescribeCluster(kafkaService).main(arrayOf("--health"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Health Status:")
        assertThat(output).contains("Under-replicated Partitions: 0")
        assertThat(output).contains("Offline Partitions:          0")
        assertThat(output).contains("Controller Active:           Yes")
        assertThat(output).contains("All Brokers Online:          Yes")
        assertThat(output).contains("Status: HEALTHY")
    }
    
    @Test
    fun `DescribeCluster with unhealthy cluster shows DEGRADED status`() {
        val cluster = ClusterInfo(
            clusterId = "abc123",
            controllerId = 1,
            controllerHost = "broker-1.kafka:9092",
            brokerCount = 3,
            topicCount = 10,
            partitionCount = 30,
            underReplicatedPartitions = 5,
            offlinePartitions = 2,
            isControllerActive = true,
            allBrokersOnline = false
        )
        every { clusterRepository.describeCluster() } returns cluster
        
        DescribeCluster(kafkaService).main(arrayOf("--health"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Under-replicated Partitions: 5")
        assertThat(output).contains("Offline Partitions:          2")
        assertThat(output).contains("All Brokers Online:          No")
        assertThat(output).contains("Status: DEGRADED")
    }
    
    @Test
    fun `DescribeCluster with topics shows topic summary`() {
        val cluster = ClusterInfo(
            clusterId = "abc123",
            controllerId = 1,
            controllerHost = "broker-1.kafka:9092",
            brokerCount = 3,
            topicCount = 10,
            partitionCount = 30
        )
        val topics = listOf(
            Topic("user-events", 3, 2),
            Topic("orders", 6, 3),
            Topic("__consumer_offsets", 50, 3, isInternal = true)
        )
        every { clusterRepository.describeCluster() } returns cluster
        every { topicRepository.listTopics(includeInternal = true, pattern = null) } returns topics
        
        DescribeCluster(kafkaService).main(arrayOf("--topics"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Topic Summary:")
        assertThat(output).contains("Internal Topics:    1")
        assertThat(output).contains("User Topics:        2")
    }
}
