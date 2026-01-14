package dev.franz.cli.commands.resources

import dev.franz.cli.kafka.KafkaService
import dev.franz.cli.kafka.model.ConsumerGroup
import dev.franz.cli.kafka.model.GroupMember
import dev.franz.cli.kafka.model.TopicSubscription
import dev.franz.cli.kafka.repository.GroupRepository
import dev.franz.cli.kafka.repository.fake.EmptyAclRepository
import dev.franz.cli.kafka.repository.fake.EmptyBrokerRepository
import dev.franz.cli.kafka.repository.fake.EmptyClusterRepository
import dev.franz.cli.kafka.repository.fake.EmptyTopicRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class GroupCommandsTest {
    
    private lateinit var groupRepository: GroupRepository
    private lateinit var kafkaService: KafkaService
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var errorStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream
    
    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        kafkaService = KafkaService(
            topics = EmptyTopicRepository(),
            brokers = EmptyBrokerRepository(),
            groups = groupRepository,
            acls = EmptyAclRepository(),
            cluster = EmptyClusterRepository()
        )
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
    fun `GetGroup lists consumer groups`() {
        val groups = listOf(
            ConsumerGroup("my-consumer-group", "Stable", members = listOf(
                GroupMember("m1", "app-1", emptyList()),
                GroupMember("m2", "app-2", emptyList())
            )),
            ConsumerGroup("analytics-consumers", "Stable", members = listOf(
                GroupMember("a1", "analytics", emptyList())
            ))
        )
        every { groupRepository.listGroups(includeEmpty = false, pattern = null) } returns groups
        
        GetGroup().main(emptyArray())
        
        val output = outputStream.toString()
        assertThat(output).contains("my-consumer-group")
        assertThat(output).contains("analytics-consumers")
        assertThat(output).contains("GROUP")
        assertThat(output).contains("STATE")
        assertThat(output).contains("MEMBERS")
        verify { groupRepository.listGroups(includeEmpty = false, pattern = null) }
    }
    
    @Test
    fun `GetGroup with show-empty includes empty groups`() {
        val groups = listOf(
            ConsumerGroup("my-group", "Stable", members = listOf(GroupMember("m1", "app", emptyList()))),
            ConsumerGroup("empty-group", "Empty", members = emptyList())
        )
        every { groupRepository.listGroups(includeEmpty = true, pattern = null) } returns groups
        
        GetGroup().main(arrayOf("--show-empty"))
        
        val output = outputStream.toString()
        assertThat(output).contains("empty-group")
        verify { groupRepository.listGroups(includeEmpty = true, pattern = null) }
    }
    
    @Test
    fun `DescribeGroup shows group details`() {
        val group = ConsumerGroup(
            name = "my-consumer-group",
            state = "Stable",
            protocolType = "consumer",
            protocol = "range",
            coordinator = "broker-1.kafka:9092 (id: 1)",
            topicSubscriptions = listOf(
                TopicSubscription("my-topic", 3),
                TopicSubscription("events", 12)
            ),
            totalLag = 1234
        )
        every { groupRepository.describeGroup("my-consumer-group") } returns group
        
        DescribeGroup().main(arrayOf("my-consumer-group"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Consumer Group: my-consumer-group")
        assertThat(output).contains("State:             Stable")
        assertThat(output).contains("Protocol Type:     consumer")
        assertThat(output).contains("my-topic (3 partitions)")
        assertThat(output).contains("1,234 messages")
        verify { groupRepository.describeGroup("my-consumer-group") }
    }
    
    @Test
    fun `DescribeGroup with members shows member details`() {
        val group = ConsumerGroup(
            name = "my-group",
            state = "Stable",
            members = listOf(
                GroupMember("consumer-1", "app-1", listOf("topic[0,1]", "events[0,1,2]")),
                GroupMember("consumer-2", "app-2", listOf("topic[2]"))
            )
        )
        every { groupRepository.describeGroup("my-group") } returns group
        
        DescribeGroup().main(arrayOf("my-group", "--members"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Members:")
        assertThat(output).contains("consumer-1 (client-id: app-1)")
        assertThat(output).contains("consumer-2 (client-id: app-2)")
        assertThat(output).contains("Assigned:")
    }
    
    @Test
    fun `DescribeGroup shows error for non-existent group`() {
        every { groupRepository.describeGroup("non-existent") } returns null
        
        DescribeGroup().main(arrayOf("non-existent"))
        
        val output = getAllOutput()
        assertThat(output).contains("not found")
    }
}
