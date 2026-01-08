package dev.franz.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.resources.DescribeGroup
import dev.franz.cli.commands.resources.DescribeTopic

class Describe : CliktCommand(
    name = "describe",
    help = "Show detailed information about a resource"
) {
    init {
        subcommands(
            DescribeTopic(),
            DescribeGroup()
        )
    }

    override fun run() = Unit
}
