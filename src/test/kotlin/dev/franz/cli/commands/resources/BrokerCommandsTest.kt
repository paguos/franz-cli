package dev.franz.cli.commands.resources

import dev.franz.cli.kafka.KafkaService
import dev.franz.cli.kafka.model.Broker
import dev.franz.cli.kafka.model.LogDir
import dev.franz.cli.kafka.repository.BrokerRepository
import dev.franz.cli.kafka.repository.fake.EmptyAclRepository
import dev.franz.cli.kafka.repository.fake.EmptyClusterRepository
import dev.franz.cli.kafka.repository.fake.EmptyGroupRepository
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

class BrokerCommandsTest {
    
    private lateinit var brokerRepository: BrokerRepository
    private lateinit var kafkaService: KafkaService
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var errorStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream
    
    @BeforeEach
    fun setUp() {
        brokerRepository = mockk()
        kafkaService = KafkaService(
            topics = EmptyTopicRepository(),
            brokers = brokerRepository,
            groups = EmptyGroupRepository(),
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
    fun `GetBroker lists all brokers`() {
        val brokers = listOf(
            Broker(1, "broker-1.kafka", 9092, "us-east-1a"),
            Broker(2, "broker-2.kafka", 9092, "us-east-1b"),
            Broker(3, "broker-3.kafka", 9092, "us-east-1c")
        )
        every { brokerRepository.listBrokers() } returns brokers
        every { brokerRepository.getControllerId() } returns 1
        
        GetBroker().main(emptyArray())
        
        val output = outputStream.toString()
        assertThat(output).contains("broker-1.kafka")
        assertThat(output).contains("broker-2.kafka")
        assertThat(output).contains("broker-3.kafka")
        assertThat(output).contains("Controller: broker-1.kafka:9092")
        verify { brokerRepository.listBrokers() }
    }
    
    @Test
    fun `GetBroker with configs shows configuration`() {
        val brokers = listOf(
            Broker(
                id = 1,
                host = "broker-1.kafka",
                port = 9092,
                configs = mapOf(
                    "log.retention.hours" to "168",
                    "num.partitions" to "3"
                )
            )
        )
        every { brokerRepository.listBrokers() } returns brokers
        every { brokerRepository.getControllerId() } returns 1
        
        GetBroker().main(arrayOf("--configs"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Common Broker Configs:")
        assertThat(output).contains("log.retention.hours=168")
    }
    
    @Test
    fun `DescribeBroker shows broker details`() {
        val broker = Broker(
            id = 1,
            host = "broker-1.kafka",
            port = 9092,
            rack = "us-east-1a",
            version = "3.6.0",
            listeners = listOf("PLAINTEXT://broker-1.kafka:9092", "SSL://broker-1.kafka:9093"),
            configs = mapOf("log.retention.hours" to "168")
        )
        every { brokerRepository.describeBroker(1) } returns broker
        
        DescribeBroker().main(arrayOf("1"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Broker: 1")
        assertThat(output).contains("Host:              broker-1.kafka")
        assertThat(output).contains("Port:              9092")
        assertThat(output).contains("Rack:              us-east-1a")
        assertThat(output).contains("PLAINTEXT://broker-1.kafka:9092")
        verify { brokerRepository.describeBroker(1) }
    }
    
    @Test
    fun `DescribeBroker with log-dirs shows directory info`() {
        val broker = Broker(
            id = 1,
            host = "broker-1.kafka",
            port = 9092,
            logDirs = listOf(
                LogDir("/var/kafka/data-1", 500L * 1024 * 1024 * 1024, 234L * 1024 * 1024 * 1024)
            )
        )
        every { brokerRepository.describeBroker(1) } returns broker
        
        DescribeBroker().main(arrayOf("1", "--log-dirs"))
        
        val output = outputStream.toString()
        assertThat(output).contains("Log Directories:")
        assertThat(output).contains("/var/kafka/data-1")
        assertThat(output).contains("Total:")
        assertThat(output).contains("Used:")
    }
    
    @Test
    fun `DescribeBroker shows error for non-existent broker`() {
        every { brokerRepository.describeBroker(999) } returns null
        
        DescribeBroker().main(arrayOf("999"))
        
        val output = getAllOutput()
        assertThat(output).contains("not found")
    }
}
