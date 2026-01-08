package dev.franz.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.resources.GetAcl
import dev.franz.cli.commands.resources.GetBroker
import dev.franz.cli.commands.resources.GetGroup
import dev.franz.cli.commands.resources.GetTopic

class Get : CliktCommand(
    name = "get",
    help = "List resources"
) {
    init {
        subcommands(
            GetTopic(),
            GetGroup(),
            GetBroker(),
            GetAcl()
        )
    }

    override fun run() = Unit
}
