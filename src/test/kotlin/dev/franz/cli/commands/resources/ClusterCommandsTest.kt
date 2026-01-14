package dev.franz.cli.commands.resources

import dev.franz.cli.kafka.KafkaService
import dev.franz.cli.kafka.model.ClusterInfo
import dev.franz.cli.kafka.model.Topic
import dev.franz.cli.kafka.repository.ClusterRepository
import dev.franz.cli.kafka.repository.TopicRepository
import dev.franz.cli.kafka.repository.fake.EmptyAclRepository
import dev.franz.cli.kafka.repository.fake.EmptyBrokerRepository
import dev.franz.cli.kafka.repository.fake.EmptyGroupRepository
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
        kafkaService = KafkaService(
            topics = topicRepository,
            brokers = EmptyBrokerRepository(),
            groups = EmptyGroupRepository(),
            acls = EmptyAclRepository(),
            cluster = clusterRepository
        )
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
        
        DescribeCluster().main(emptyArray())
        
        val output = outputStream.toString()
        assertThat(output).contains("Cluster ID:")
        assertThat(output).contains("abc123-def456")
        assertThat(output).contains("Controller:")
        assertThat(output).contains("broker-1.kafka:9092 (id: 1)")
        assertThat(output).contains("Brokers:")
        assertThat(output).contains("3")
        assertThat(output).contains("Topics:")
        assertThat(output).contains("42")
        assertThat(output).contains("Kafka Version:")
        assertThat(output).contains("3.6.0")
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
        
        DescribeCluster().main(arrayOf("--health"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Health Status:")
        assertThat(output).contains("Under-replicated Partitions:")
        assertThat(output).contains("Offline Partitions:")
        assertThat(output).contains("Controller Active:")
        assertThat(output).contains("All Brokers Online:")
        assertThat(output).contains("Status:")
        assertThat(output).contains("HEALTHY")
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
        
        DescribeCluster().main(arrayOf("--health"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Under-replicated Partitions:")
        assertThat(output).contains("5")
        assertThat(output).contains("Offline Partitions:")
        assertThat(output).contains("2")
        assertThat(output).contains("All Brokers Online:")
        assertThat(output).contains("No")
        assertThat(output).contains("Status:")
        assertThat(output).contains("DEGRADED")
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
        
        DescribeCluster().main(arrayOf("--topics"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Topic Summary:")
        assertThat(output).contains("Internal Topics:")
        assertThat(output).contains("1")
        assertThat(output).contains("User Topics:")
        assertThat(output).contains("2")
    }
}
