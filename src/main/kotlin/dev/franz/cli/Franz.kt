package dev.franz.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.franz.cli.commands.Create
import dev.franz.cli.commands.Delete
import dev.franz.cli.commands.Describe
import dev.franz.cli.commands.Get
import dev.franz.cli.kafka.KafkaService

class Franz : CliktCommand(
    name = "franz",
    help = "A CLI tool for interacting with Apache Kafka"
) {
    private val bootstrapServers by option(
        "--bootstrap-servers", "-b",
        help = "Kafka bootstrap servers (e.g., localhost:9092). " +
               "Can also be set via KAFKA_BOOTSTRAP_SERVERS env var. " +
               "If not set, uses mock data.",
        envvar = "KAFKA_BOOTSTRAP_SERVERS"
    )
    
    private val mock by option(
        "--mock",
        help = "Use mock data instead of connecting to Kafka"
    ).default("false")

    init {
        versionOption("0.1.0")
        subcommands(
            Get(),
            Describe(),
            Create(),
            Delete()
        )
    }

    override fun run() {
        // Configure KafkaService based on options
        if (mock == "true" || bootstrapServers == null) {
            KafkaService.configureMock()
        } else {
            KafkaService.configure(bootstrapServers!!)
        }
    }
}
