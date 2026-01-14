package dev.franz.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.resources.DescribeBroker
import dev.franz.cli.commands.resources.DescribeCluster
import dev.franz.cli.commands.resources.DescribeGroup
import dev.franz.cli.commands.resources.DescribeTopic

class Describe : CliktCommand(
    name = "describe",
    help = """
        Show detailed information about a Kafka resource.

        Examples:
        ```
        franz describe topic my-topic
        franz describe group my-consumer-group --members
        franz describe broker 1
        franz describe cluster --health
        ```
    """.trimIndent(),
    invokeWithoutSubcommand = true,
    printHelpOnEmptyArgs = true
) {
    init {
        subcommands(
            DescribeTopic(),
            DescribeGroup(),
            DescribeBroker(),
            DescribeCluster()
        )
    }

    override fun run() = Unit
}
