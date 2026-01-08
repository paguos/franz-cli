package dev.franz.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.config.*

class Config : CliktCommand(
    name = "config",
    help = "Manage Franz CLI configuration (contexts, clusters, credentials)"
) {
    init {
        subcommands(
            GetContexts(),
            UseContext(),
            CurrentContext(),
            SetContext(),
            SetCluster(),
            SetCredentials(),
            DeleteContext(),
            ViewConfig()
        )
    }
    
    override fun run() = Unit
}
