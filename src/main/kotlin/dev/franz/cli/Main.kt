package dev.franz.cli

private const val SIMPLE_LOG_LEVEL_PROP = "org.slf4j.simpleLogger.defaultLogLevel"

fun main(args: Array<String>) {
    // Allow `--debug` anywhere in the argv (kubectl-style global flag).
    // We strip it before Clikt parsing so subcommands don't reject it as an unknown option.
    var debug = false
    val filteredArgs = args.filter { token ->
        when (token) {
            "--debug" -> {
                debug = true
                false
            }

            "--debug=true" -> {
                debug = true
                false
            }

            "--debug=false" -> {
                debug = false
                false
            }

            else -> true
        }
    }.toTypedArray()

    // Set logging verbosity as early as possible so Kafka client initialization picks it up.
    // If something already set this property, do not override it.
    if (System.getProperty(SIMPLE_LOG_LEVEL_PROP) == null) {
        System.setProperty(SIMPLE_LOG_LEVEL_PROP, if (debug) "info" else "warn")
    }

    Franz().main(filteredArgs)
}
