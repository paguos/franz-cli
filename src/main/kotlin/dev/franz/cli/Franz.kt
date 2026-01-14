package dev.franz.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.eagerOption
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.mordant.terminal.Terminal
import dev.franz.cli.commands.Config
import dev.franz.cli.commands.Create
import dev.franz.cli.commands.Delete
import dev.franz.cli.commands.Describe
import dev.franz.cli.commands.Get
import dev.franz.cli.config.ConfigException
import dev.franz.cli.kafka.KafkaService

class Franz : CliktCommand(
    name = "franz",
    help = """
        A CLI tool for interacting with Apache Kafka.

        Franz uses a kubeconfig-like configuration file with contexts to select a cluster and credentials.
        Use `franz config` to create clusters, credentials, and contexts.

        Examples:
        ```
        franz config set-cluster local -b localhost:9092
        franz config set-context local --cluster local
        franz config use-context local
        franz get topic
        franz describe cluster --health
        ```
    """.trimIndent(),
    invokeWithoutSubcommand = true,
    printHelpOnEmptyArgs = true
) {
    private val contextName by option(
        "--context", "-c",
        help = "Name of the context to use (overrides current-context from config)"
    )
    private var debug: Boolean = false

    init {
        eagerOption(
            "--debug",
            help = "Enable debug logging (shows Kafka client logs)"
        ) {
            debug = true
        }

        // Work around broken terminal width detection in some native-image builds, which can cause
        // help text to wrap at 1 column (one word per line).
        context {
            terminal = Terminal()
            terminal.info.updateTerminalSize()
            if (terminal.info.width < 40) terminal.info.width = 80
        }

        val implVersion = Franz::class.java.`package`?.implementationVersion
        versionOption(implVersion ?: "dev")
        subcommands(
            Get(),
            Describe(),
            Create(),
            Delete(),
            Config()
        )
    }

    override fun run() {
        // If no subcommand was invoked, Clikt will print help (printHelpOnEmptyArgs=true).
        // Avoid configuring Kafka in that case to keep `franz` side-effect free.
        if (currentContext.invokedSubcommand == null) return

        // Configure SimpleLogger before any Kafka client initialization.
        // If `main()` already set it (e.g. when --debug is used after subcommands), don't override.
        if (System.getProperty("org.slf4j.simpleLogger.defaultLogLevel") == null) {
            System.setProperty(
                "org.slf4j.simpleLogger.defaultLogLevel",
                if (debug) "info" else "warn"
            )
        }

        // Do not require a Kafka context for configuration commands
        if (currentContext.invokedSubcommand?.commandName == "config") return

        // All Kafka commands require a valid context (via --context or current-context)
        try {
            KafkaService.configureFromContext(contextName)
        } catch (e: ConfigException) {
            currentContext.fail(e.message ?: "Failed to resolve Kafka context.")
        }
    }
}
