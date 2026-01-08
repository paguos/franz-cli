package dev.franz.cli.kafka.repository.integration

import dev.franz.cli.kafka.repository.kafka.KafkaBrokerRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for KafkaBrokerRepository.
 * Tests run against a real Kafka instance via Testcontainers.
 */
class KafkaBrokerRepositoryIT : KafkaTestBase() {
    
    private lateinit var repository: KafkaBrokerRepository
    
    @BeforeEach
    fun setUp() {
        repository = KafkaBrokerRepository(adminClient)
    }
    
    @Test
    fun `listBrokers returns all brokers in cluster`() {
        // When: list brokers
        val brokers = repository.listBrokers()
        
        // Then: at least one broker exists (testcontainer runs single node)
        assertThat(brokers).isNotEmpty
        
        val broker = brokers.first()
        assertThat(broker.id).isGreaterThanOrEqualTo(0)
        assertThat(broker.host).isNotBlank()
        assertThat(broker.port).isGreaterThan(0)
    }
    
    @Test
    fun `describeBroker returns broker details`() {
        // Given: get a broker ID first
        val brokers = repository.listBrokers()
        val brokerId = brokers.first().id
        
        // When: describe the broker
        val broker = repository.describeBroker(brokerId)
        
        // Then: broker details are returned
        assertThat(broker).isNotNull
        assertThat(broker!!.id).isEqualTo(brokerId)
        assertThat(broker.host).isNotBlank()
        assertThat(broker.port).isGreaterThan(0)
    }
    
    @Test
    fun `describeBroker returns null for non-existent broker`() {
        // When: describe a non-existent broker
        val broker = repository.describeBroker(99999)
        
        // Then: null is returned
        assertThat(broker).isNull()
    }
    
    @Test
    fun `getControllerId returns valid controller ID`() {
        // When: get controller ID
        val controllerId = repository.getControllerId()
        
        // Then: controller ID is valid (non-negative)
        assertThat(controllerId).isGreaterThanOrEqualTo(0)
        
        // And: it matches one of the brokers
        val brokers = repository.listBrokers()
        assertThat(brokers.map { it.id }).contains(controllerId)
    }
}
