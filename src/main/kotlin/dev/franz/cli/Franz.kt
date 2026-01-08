package dev.franz.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.franz.cli.commands.Config
import dev.franz.cli.commands.Create
import dev.franz.cli.commands.Delete
import dev.franz.cli.commands.Describe
import dev.franz.cli.commands.Get
import dev.franz.cli.kafka.KafkaService

class Franz : CliktCommand(
    name = "franz",
    help = "A CLI tool for interacting with Apache Kafka"
) {
    private val context by option(
        "--context", "-c",
        help = "Name of the context to use (overrides current-context from config)"
    )
    
    private val mock by option(
        "--mock",
        help = "Use mock data instead of connecting to Kafka"
    ).flag()

    init {
        versionOption("0.1.0")
        subcommands(
            Get(),
            Describe(),
            Create(),
            Delete(),
            Config()
        )
    }

    override fun run() {
        // Configure KafkaService based on options
        if (mock) {
            KafkaService.configureMock()
        } else {
            // Use context-based configuration
            // If no context specified and no current context, commands will fail with helpful message
            try {
                KafkaService.configureFromContext(context)
            } catch (e: Exception) {
                // Don't fail here - let the subcommand handle it
                // This allows 'config' commands to work without a context
                KafkaService.configureMock()
            }
        }
    }
}
