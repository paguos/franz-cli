package dev.franz.cli

import com.github.ajalt.clikt.core.CliktCommand
import dev.franz.cli.output.OutputFormatter

open class FranzCommand(
    help: String = "",
    epilog: String = "",
    name: String? = null,
    invokeWithoutSubcommand: Boolean = false,
    printHelpOnEmptyArgs: Boolean = false,
    helpTags: Map<String, String> = emptyMap()
) : CliktCommand(
    help = help,
    epilog = epilog,
    name = name,
    invokeWithoutSubcommand = invokeWithoutSubcommand,
    printHelpOnEmptyArgs = printHelpOnEmptyArgs,
    helpTags = helpTags
) {
    protected val output = OutputFormatter { line -> echo(line) }

    protected fun errorLine(text: String) {
        echo(text, err = true)
    }

    override fun run() = Unit
}
