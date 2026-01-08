package dev.franz.cli.kafka

import dev.franz.cli.config.ConfigManager
import dev.franz.cli.config.KafkaPropertiesBuilder
import dev.franz.cli.kafka.repository.AclRepository
import dev.franz.cli.kafka.repository.BrokerRepository
import dev.franz.cli.kafka.repository.ClusterRepository
import dev.franz.cli.kafka.repository.GroupRepository
import dev.franz.cli.kafka.repository.TopicRepository
import dev.franz.cli.kafka.repository.kafka.KafkaAclRepository
import dev.franz.cli.kafka.repository.kafka.KafkaBrokerRepository
import dev.franz.cli.kafka.repository.kafka.KafkaClusterRepository
import dev.franz.cli.kafka.repository.kafka.KafkaGroupRepository
import dev.franz.cli.kafka.repository.kafka.KafkaTopicRepository
import dev.franz.cli.kafka.repository.mock.MockAclRepository
import dev.franz.cli.kafka.repository.mock.MockBrokerRepository
import dev.franz.cli.kafka.repository.mock.MockClusterRepository
import dev.franz.cli.kafka.repository.mock.MockGroupRepository
import dev.franz.cli.kafka.repository.mock.MockTopicRepository
import org.apache.kafka.clients.admin.AdminClient
import java.util.Properties

/**
 * Facade for all Kafka operations.
 * Aggregates all repositories and provides a single entry point for commands.
 */
class KafkaService(
    val topics: TopicRepository,
    val brokers: BrokerRepository,
    val groups: GroupRepository,
    val acls: AclRepository,
    val cluster: ClusterRepository,
    private val adminClient: AdminClient? = null
) {
    /**
     * Close the underlying AdminClient if one exists
     */
    fun close() {
        adminClient?.close()
    }
    
    companion object {
        private var instance: KafkaService? = null
        
        /**
         * Get the current KafkaService instance.
         * If not configured, returns a mock instance.
         */
        fun getInstance(): KafkaService {
            if (instance == null) {
                instance = createMockService()
            }
            return instance!!
        }
        
        /**
         * Configure KafkaService from a named context.
         * Uses the current context if contextName is null.
         * 
         * @param contextName Name of the context to use, or null for current context
         * @param configManager ConfigManager instance (for testing)
         * @param propertiesBuilder KafkaPropertiesBuilder instance (for testing)
         */
        fun configureFromContext(
            contextName: String?,
            configManager: ConfigManager = ConfigManager(),
            propertiesBuilder: KafkaPropertiesBuilder = KafkaPropertiesBuilder()
        ) {
            instance?.close()
            
            val resolvedContext = configManager.resolveContext(contextName)
            val props = propertiesBuilder.build(resolvedContext)
            
            instance = createRealService(props)
        }
        
        /**
         * Configure KafkaService to use mock implementations.
         */
        fun configureMock() {
            instance?.close()
            instance = createMockService()
        }
        
        /**
         * For testing - allows injecting a custom instance.
         */
        fun setInstance(service: KafkaService) {
            instance?.close()
            instance = service
        }
        
        fun resetInstance() {
            instance?.close()
            instance = null
        }
        
        private fun createMockService(): KafkaService {
            return KafkaService(
                topics = MockTopicRepository(),
                brokers = MockBrokerRepository(),
                groups = MockGroupRepository(),
                acls = MockAclRepository(),
                cluster = MockClusterRepository()
            )
        }
        
        private fun createRealService(props: Properties): KafkaService {
            val adminClient = AdminClient.create(props)
            
            return KafkaService(
                topics = KafkaTopicRepository(adminClient),
                brokers = KafkaBrokerRepository(adminClient),
                groups = KafkaGroupRepository(adminClient),
                acls = KafkaAclRepository(adminClient),
                cluster = KafkaClusterRepository(adminClient),
                adminClient = adminClient
            )
        }
    }
}
