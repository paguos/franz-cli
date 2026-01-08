package dev.franz.cli.kafka.repository.mock

import dev.franz.cli.kafka.model.ClusterInfo
import dev.franz.cli.kafka.repository.ClusterRepository

class MockClusterRepository : ClusterRepository {
    
    override fun describeCluster(): ClusterInfo {
        return ClusterInfo(
            clusterId = "abc123-def456-ghi789",
            controllerId = 1,
            controllerHost = "broker-1.kafka:9092",
            brokerCount = 3,
            topicCount = 42,
            partitionCount = 156,
            kafkaVersion = "3.6.0",
            protocolVersion = "3.6",
            underReplicatedPartitions = 0,
            offlinePartitions = 0,
            isControllerActive = true,
            allBrokersOnline = true
        )
    }
}
