package dev.franz.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.resources.DeleteAcl
import dev.franz.cli.commands.resources.DeleteTopic

class Delete : CliktCommand(
    name = "delete",
    help = "Delete resources"
) {
    init {
        subcommands(
            DeleteTopic(),
            DeleteAcl()
        )
    }

    override fun run() = Unit
}
