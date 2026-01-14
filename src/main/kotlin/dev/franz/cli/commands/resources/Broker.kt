package dev.franz.cli.commands.resources

import dev.franz.cli.FranzCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import dev.franz.cli.kafka.KafkaService

class GetBroker : FranzCommand(
    name = "broker",
    help = """
        List Kafka brokers.

        Examples:
        ```
        franz get broker
        franz get broker --configs
        ```
    """.trimIndent()
) {
    private val showConfigs by option("--configs", "-c", help = "Show broker configurations").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        val brokers = kafka.brokers.listBrokers()
        val controllerId = kafka.brokers.getControllerId()

        output.table(
            headers = listOf("ID", "HOST", "PORT", "RACK"),
            rows = brokers.map { broker ->
                listOf(
                    broker.id.toString(),
                    broker.host,
                    broker.port.toString(),
                    broker.rack ?: "N/A"
                )
            }
        )

        val controller = brokers.find { it.id == controllerId }
        if (controller != null) {
            output.line()
            output.kvTable(
                listOf(
                    "Controller" to "${controller.host}:${controller.port} (id: ${controller.id})"
                )
            )
        }
        
        if (showConfigs && brokers.isNotEmpty()) {
            val firstBroker = brokers.first()
            if (firstBroker.configs.isNotEmpty()) {
                output.line()
                output.section("Common Broker Configs")
                output.table(
                    headers = listOf("NAME", "VALUE"),
                    rows = firstBroker.configs.entries.take(3).map { (key, value) ->
                        listOf(key, value)
                    }
                )
            }
        }
    }
}

class DescribeBroker : FranzCommand(
    name = "broker",
    help = """
        Show detailed information about a broker.

        Examples:
        ```
        franz describe broker 1
        franz describe broker 1 --log-dirs
        ```
    """.trimIndent()
) {
    private val id by argument(help = "Broker ID").int()
    private val showLogDirs by option("--log-dirs", "-l", help = "Show log directory details").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        val broker = kafka.brokers.describeBroker(id)
        
        if (broker == null) {
            errorLine("Broker '$id' not found.")
            return
        }

        output.kvTable(
            listOf(
                "ID" to broker.id.toString(),
                "Host" to broker.host,
                "Port" to broker.port.toString(),
                "Rack" to (broker.rack ?: "N/A"),
                "Version" to broker.version
            )
        )

        if (broker.listeners.isNotEmpty()) {
            output.line()
            output.section("Listeners")
            output.table(
                headers = listOf("LISTENER"),
                rows = broker.listeners.map { listener -> listOf(listener) }
            )
        }
        
        if (broker.configs.isNotEmpty()) {
            output.line()
            output.section("Configurations")
            output.table(
                headers = listOf("NAME", "VALUE"),
                rows = broker.configs.map { (key, value) -> listOf(key, value) }
            )
        }
        
        if (showLogDirs && broker.logDirs.isNotEmpty()) {
            output.line()
            output.section("Log Directories")
            output.table(
                headers = listOf("PATH", "TOTAL_GB", "USED_GB", "AVAILABLE_GB", "USED_PCT"),
                rows = broker.logDirs.map { logDir ->
                    val totalGb = logDir.totalBytes / (1024 * 1024 * 1024)
                    val usedGb = logDir.usedBytes / (1024 * 1024 * 1024)
                    val availableGb = logDir.availableBytes / (1024 * 1024 * 1024)
                    listOf(
                        logDir.path,
                        totalGb.toString(),
                        usedGb.toString(),
                        availableGb.toString(),
                        "%.1f".format(logDir.usedPercent)
                    )
                }
            )
        }
    }
}
