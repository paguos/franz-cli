package dev.franz.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.resources.DeleteAcl
import dev.franz.cli.commands.resources.DeleteTopic

class Delete : CliktCommand(
    name = "delete",
    help = """
        Delete Kafka resources.

        Examples:
        ```
        franz delete topic my-topic --force
        franz delete acl --principal User:alice --resource-type topic --resource-name my-topic --operation Read --force
        ```
    """.trimIndent(),
    invokeWithoutSubcommand = true,
    printHelpOnEmptyArgs = true
) {
    init {
        subcommands(
            DeleteTopic(),
            DeleteAcl()
        )
    }

    override fun run() = Unit
}
