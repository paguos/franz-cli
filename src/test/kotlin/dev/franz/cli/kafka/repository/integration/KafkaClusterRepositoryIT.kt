package dev.franz.cli.kafka.repository.integration

import dev.franz.cli.kafka.repository.kafka.KafkaClusterRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Integration tests for KafkaClusterRepository.
 * Tests run against a real Kafka instance via Testcontainers.
 */
@Tag("integration")
class KafkaClusterRepositoryIT : KafkaTestBase() {
    
    private lateinit var repository: KafkaClusterRepository
    
    @BeforeEach
    fun setUp() {
        repository = KafkaClusterRepository(adminClient)
    }
    
    @Test
    fun `describeCluster returns cluster information`() {
        // When: describe cluster
        val cluster = repository.describeCluster()
        
        // Then: cluster info is populated
        assertThat(cluster.clusterId).isNotBlank()
        assertThat(cluster.controllerId).isGreaterThanOrEqualTo(0)
        assertThat(cluster.controllerHost).isNotBlank()
        assertThat(cluster.brokerCount).isGreaterThan(0)
    }
    
    @Test
    fun `describeCluster has valid controller info`() {
        // When: describe cluster
        val cluster = repository.describeCluster()
        
        // Then: controller info is valid
        assertThat(cluster.controllerId).isGreaterThanOrEqualTo(0)
        assertThat(cluster.controllerHost).contains(":")  // host:port format
    }
    
    @Test
    fun `describeCluster reports healthy cluster`() {
        // When: describe cluster
        val cluster = repository.describeCluster()
        
        // Then: cluster appears healthy (testcontainer is single node)
        assertThat(cluster.isControllerActive).isTrue()
        assertThat(cluster.allBrokersOnline).isTrue()
    }
}
