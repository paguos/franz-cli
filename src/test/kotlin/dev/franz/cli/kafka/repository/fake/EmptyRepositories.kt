package dev.franz.cli.kafka.repository.fake

import dev.franz.cli.kafka.model.Acl
import dev.franz.cli.kafka.model.AclOperation
import dev.franz.cli.kafka.model.AclPermission
import dev.franz.cli.kafka.model.Broker
import dev.franz.cli.kafka.model.ClusterInfo
import dev.franz.cli.kafka.model.ConsumerGroup
import dev.franz.cli.kafka.model.PatternType
import dev.franz.cli.kafka.model.ResourceType
import dev.franz.cli.kafka.model.Topic
import dev.franz.cli.kafka.repository.AclRepository
import dev.franz.cli.kafka.repository.BrokerRepository
import dev.franz.cli.kafka.repository.ClusterRepository
import dev.franz.cli.kafka.repository.GroupRepository
import dev.franz.cli.kafka.repository.TopicRepository

/**
 * Test-only "empty" repository implementations used to satisfy KafkaService wiring in unit tests.
 * They intentionally return empty/no-op results.
 */

class EmptyTopicRepository : TopicRepository {
    override fun listTopics(includeInternal: Boolean, pattern: String?): List<Topic> = emptyList()
    override fun describeTopic(name: String): Topic? = null
    override fun deleteTopic(name: String): Boolean = false
    override fun createTopic(name: String, partitions: Int, replicationFactor: Int): Topic =
        Topic(name = name, partitions = partitions, replicationFactor = replicationFactor)
}

class EmptyBrokerRepository : BrokerRepository {
    override fun listBrokers(): List<Broker> = emptyList()
    override fun describeBroker(id: Int): Broker? = null
    override fun getControllerId(): Int = -1
}

class EmptyGroupRepository : GroupRepository {
    override fun listGroups(includeEmpty: Boolean, pattern: String?): List<ConsumerGroup> = emptyList()
    override fun describeGroup(name: String): ConsumerGroup? = null
    override fun deleteGroup(name: String): Boolean = false
}

class EmptyAclRepository : AclRepository {
    override fun listAcls(principal: String?, resourceType: ResourceType?, resourceName: String?): List<Acl> =
        emptyList()

    override fun createAcl(
        principal: String,
        resourceType: ResourceType,
        resourceName: String,
        operation: AclOperation,
        permission: AclPermission,
        patternType: PatternType
    ): Acl = Acl(
        principal = principal,
        resourceType = resourceType,
        resourceName = resourceName,
        operation = operation,
        permission = permission,
        patternType = patternType
    )

    override fun deleteAcls(
        principal: String?,
        resourceType: ResourceType?,
        resourceName: String?,
        operation: AclOperation?
    ): List<Acl> = emptyList()
}

class EmptyClusterRepository : ClusterRepository {
    override fun describeCluster(): ClusterInfo = ClusterInfo(
        clusterId = "unknown",
        controllerId = -1,
        controllerHost = "unknown",
        brokerCount = 0,
        topicCount = 0,
        partitionCount = 0,
        kafkaVersion = "unknown",
        protocolVersion = "unknown",
        underReplicatedPartitions = 0,
        offlinePartitions = 0,
        isControllerActive = false,
        allBrokersOnline = false
    )
}

