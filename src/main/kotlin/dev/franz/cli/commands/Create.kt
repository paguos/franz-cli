package dev.franz.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.resources.CreateAcl

class Create : CliktCommand(
    name = "create",
    help = """
        Create Kafka resources.

        Examples:
        ```
        franz create acl --principal User:alice --resource-type topic --resource-name my-topic --operation Read --permission Allow
        ```
    """.trimIndent(),
    invokeWithoutSubcommand = true,
    printHelpOnEmptyArgs = true
) {
    init {
        subcommands(
            CreateAcl()
        )
    }

    override fun run() = Unit
}
