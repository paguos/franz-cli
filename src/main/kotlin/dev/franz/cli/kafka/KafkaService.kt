package dev.franz.cli.kafka

import dev.franz.cli.kafka.repository.AclRepository
import dev.franz.cli.kafka.repository.BrokerRepository
import dev.franz.cli.kafka.repository.ClusterRepository
import dev.franz.cli.kafka.repository.GroupRepository
import dev.franz.cli.kafka.repository.TopicRepository
import dev.franz.cli.kafka.repository.mock.MockAclRepository
import dev.franz.cli.kafka.repository.mock.MockBrokerRepository
import dev.franz.cli.kafka.repository.mock.MockClusterRepository
import dev.franz.cli.kafka.repository.mock.MockGroupRepository
import dev.franz.cli.kafka.repository.mock.MockTopicRepository

/**
 * Facade for all Kafka operations.
 * Aggregates all repositories and provides a single entry point for commands.
 */
class KafkaService(
    val topics: TopicRepository = MockTopicRepository(),
    val brokers: BrokerRepository = MockBrokerRepository(),
    val groups: GroupRepository = MockGroupRepository(),
    val acls: AclRepository = MockAclRepository(),
    val cluster: ClusterRepository = MockClusterRepository()
) {
    companion object {
        // Default instance using mock implementations
        private var instance: KafkaService? = null
        
        fun getInstance(): KafkaService {
            if (instance == null) {
                instance = KafkaService()
            }
            return instance!!
        }
        
        // For testing - allows injecting a custom instance
        fun setInstance(service: KafkaService) {
            instance = service
        }
        
        fun resetInstance() {
            instance = null
        }
    }
}
