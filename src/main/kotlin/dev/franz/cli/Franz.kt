package dev.franz.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.Delete
import dev.franz.cli.commands.Describe
import dev.franz.cli.commands.Get

class Franz : CliktCommand(
    name = "franz",
    help = "A CLI tool for interacting with Apache Kafka"
) {
    init {
        subcommands(
            Get(),
            Describe(),
            Delete()
        )
    }

    override fun run() = Unit
}
