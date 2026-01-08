package dev.franz.cli.kafka.repository.mock

import dev.franz.cli.kafka.model.Broker
import dev.franz.cli.kafka.model.LogDir
import dev.franz.cli.kafka.repository.BrokerRepository

class MockBrokerRepository : BrokerRepository {
    
    private val brokers = listOf(
        Broker(
            id = 1,
            host = "broker-1.kafka",
            port = 9092,
            rack = "us-east-1a",
            listeners = listOf("PLAINTEXT://broker-1.kafka:9092", "SSL://broker-1.kafka:9093"),
            configs = mapOf(
                "log.retention.hours" to "168",
                "log.segment.bytes" to "1073741824",
                "num.io.threads" to "8",
                "num.network.threads" to "3",
                "num.replica.fetchers" to "1"
            ),
            logDirs = listOf(
                LogDir("/var/kafka/data-1", 500L * 1024 * 1024 * 1024, 234L * 1024 * 1024 * 1024),
                LogDir("/var/kafka/data-2", 500L * 1024 * 1024 * 1024, 198L * 1024 * 1024 * 1024)
            )
        ),
        Broker(
            id = 2,
            host = "broker-2.kafka",
            port = 9092,
            rack = "us-east-1b",
            listeners = listOf("PLAINTEXT://broker-2.kafka:9092", "SSL://broker-2.kafka:9093")
        ),
        Broker(
            id = 3,
            host = "broker-3.kafka",
            port = 9092,
            rack = "us-east-1c",
            listeners = listOf("PLAINTEXT://broker-3.kafka:9092", "SSL://broker-3.kafka:9093")
        )
    )
    
    override fun listBrokers(): List<Broker> = brokers
    
    override fun describeBroker(id: Int): Broker? = brokers.find { it.id == id }
    
    override fun getControllerId(): Int = 1
}
