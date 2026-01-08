package dev.franz.cli.kafka.model

data class Partition(
    val id: Int,
    val leader: Int,
    val replicas: List<Int>,
    val isr: List<Int>
)

data class Topic(
    val name: String,
    val partitions: Int,
    val replicationFactor: Int,
    val isInternal: Boolean = false,
    val cleanupPolicy: String = "delete",
    val retentionMs: Long = 604800000,
    val partitionDetails: List<Partition> = emptyList()
)

data class Broker(
    val id: Int,
    val host: String,
    val port: Int,
    val rack: String? = null,
    val version: String = "3.6.0",
    val listeners: List<String> = emptyList(),
    val configs: Map<String, String> = emptyMap(),
    val logDirs: List<LogDir> = emptyList()
)

data class LogDir(
    val path: String,
    val totalBytes: Long,
    val usedBytes: Long
) {
    val availableBytes: Long get() = totalBytes - usedBytes
    val usedPercent: Double get() = (usedBytes.toDouble() / totalBytes) * 100
}

data class ConsumerGroup(
    val name: String,
    val state: String,
    val members: List<GroupMember> = emptyList(),
    val protocolType: String = "consumer",
    val protocol: String = "range",
    val coordinator: String = "",
    val topicSubscriptions: List<TopicSubscription> = emptyList(),
    val totalLag: Long = 0
)

data class GroupMember(
    val memberId: String,
    val clientId: String,
    val assignments: List<String> = emptyList()
)

data class TopicSubscription(
    val topic: String,
    val partitions: Int
)

data class Acl(
    val principal: String,
    val resourceType: ResourceType,
    val resourceName: String,
    val patternType: PatternType = PatternType.LITERAL,
    val operation: AclOperation,
    val permission: AclPermission
)

enum class ResourceType {
    TOPIC, GROUP, CLUSTER, TRANSACTIONAL_ID
}

enum class PatternType {
    LITERAL, PREFIXED
}

enum class AclOperation {
    READ, WRITE, CREATE, DELETE, ALTER, DESCRIBE, ALL
}

enum class AclPermission {
    ALLOW, DENY
}

data class ClusterInfo(
    val clusterId: String,
    val controllerId: Int,
    val controllerHost: String,
    val brokerCount: Int,
    val topicCount: Int,
    val partitionCount: Int,
    val kafkaVersion: String = "3.6.0",
    val protocolVersion: String = "3.6",
    val underReplicatedPartitions: Int = 0,
    val offlinePartitions: Int = 0,
    val isControllerActive: Boolean = true,
    val allBrokersOnline: Boolean = true
)
