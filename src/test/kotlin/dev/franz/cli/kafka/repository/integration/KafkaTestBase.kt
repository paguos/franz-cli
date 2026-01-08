package dev.franz.cli.kafka.repository.integration

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.Properties

/**
 * Base class for Kafka integration tests.
 * Provides a shared KafkaContainer and AdminClient for all tests.
 */
@Testcontainers
abstract class KafkaTestBase {
    
    companion object {
        @Container
        @JvmStatic
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
        
        @JvmStatic
        lateinit var adminClient: AdminClient
        
        @BeforeAll
        @JvmStatic
        fun setUpKafka() {
            kafka.start()
            
            val props = Properties().apply {
                put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
                put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000)
                put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 30000)
            }
            adminClient = AdminClient.create(props)
        }
        
        @AfterAll
        @JvmStatic
        fun tearDownKafka() {
            if (::adminClient.isInitialized) {
                adminClient.close()
            }
        }
    }
    
    /**
     * Get bootstrap servers for creating repositories
     */
    fun getBootstrapServers(): String = kafka.bootstrapServers
}
