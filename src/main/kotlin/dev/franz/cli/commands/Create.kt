package dev.franz.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.resources.CreateAcl

class Create : CliktCommand(
    name = "create",
    help = "Create resources"
) {
    init {
        subcommands(
            CreateAcl()
        )
    }

    override fun run() = Unit
}
