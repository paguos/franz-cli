package dev.franz.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.franz.cli.commands.config.*

class Config : CliktCommand(
    name = "config",
    help = """
        Manage Franz CLI configuration (contexts, clusters, credentials).

        Examples:
        ```
        franz config set-cluster local -b localhost:9092
        franz config set-credentials local --security-protocol PLAINTEXT
        franz config set-context local --cluster local --auth local
        franz config use-context local
        franz config view
        ```
    """.trimIndent(),
    invokeWithoutSubcommand = true,
    printHelpOnEmptyArgs = true
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
