package dev.franz.cli.kafka.repository

import dev.franz.cli.kafka.model.ClusterInfo

interface ClusterRepository {
    fun describeCluster(): ClusterInfo
}
