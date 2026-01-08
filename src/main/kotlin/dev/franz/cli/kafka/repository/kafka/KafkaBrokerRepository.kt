package dev.franz.cli.kafka.repository.kafka

import dev.franz.cli.kafka.model.Broker
import dev.franz.cli.kafka.repository.BrokerRepository
import org.apache.kafka.clients.admin.AdminClient

/**
 * Real Kafka implementation of BrokerRepository.
 * Uses Kafka AdminClient to interact with the cluster.
 */
class KafkaBrokerRepository(
    private val adminClient: AdminClient
) : BrokerRepository {
    
    override fun listBrokers(): List<Broker> {
        val clusterResult = adminClient.describeCluster()
        val nodes = clusterResult.nodes().get()
        
        return nodes.map { node ->
            Broker(
                id = node.id(),
                host = node.host(),
                port = node.port(),
                rack = node.rack()
            )
        }
    }
    
    override fun describeBroker(id: Int): Broker? {
        val brokers = listBrokers()
        return brokers.find { it.id == id }
    }
    
    override fun getControllerId(): Int {
        val clusterResult = adminClient.describeCluster()
        return clusterResult.controller().get().id()
    }
}
