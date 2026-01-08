package dev.franz.cli.kafka.repository

import dev.franz.cli.kafka.model.Broker

interface BrokerRepository {
    fun listBrokers(): List<Broker>
    fun describeBroker(id: Int): Broker?
    fun getControllerId(): Int
}
