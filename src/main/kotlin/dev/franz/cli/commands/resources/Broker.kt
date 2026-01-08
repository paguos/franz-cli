package dev.franz.cli.commands.resources

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import dev.franz.cli.kafka.KafkaService

class GetBroker(
    private val kafka: KafkaService = KafkaService.getInstance()
) : CliktCommand(
    name = "broker",
    help = "List Kafka brokers"
) {
    private val showConfigs by option("--configs", "-c", help = "Show broker configurations").flag()

    override fun run() {
        echo("Listing brokers...")
        echo()
        
        val brokers = kafka.brokers.listBrokers()
        val controllerId = kafka.brokers.getControllerId()
        
        echo("ID    HOST                PORT    RACK")
        brokers.forEach { broker ->
            echo("${broker.id.toString().padEnd(5)} ${broker.host.padEnd(19)} ${broker.port.toString().padEnd(7)} ${broker.rack ?: "N/A"}")
        }
        echo()
        
        val controller = brokers.find { it.id == controllerId }
        if (controller != null) {
            echo("Controller: ${controller.host}:${controller.port} (id: ${controller.id})")
        }
        
        if (showConfigs && brokers.isNotEmpty()) {
            val firstBroker = brokers.first()
            if (firstBroker.configs.isNotEmpty()) {
                echo()
                echo("Common Broker Configs:")
                firstBroker.configs.entries.take(3).forEach { (key, value) ->
                    echo("  $key=$value")
                }
            }
        }
    }
}

class DescribeBroker(
    private val kafka: KafkaService = KafkaService.getInstance()
) : CliktCommand(
    name = "broker",
    help = "Show detailed information about a broker"
) {
    private val id by argument(help = "Broker ID").int()
    private val showLogDirs by option("--log-dirs", "-l", help = "Show log directory details").flag()

    override fun run() {
        val broker = kafka.brokers.describeBroker(id)
        
        if (broker == null) {
            echo("Broker '$id' not found.", err = true)
            return
        }
        
        echo("Broker: ${broker.id}")
        echo("=".repeat(50))
        echo("Host:              ${broker.host}")
        echo("Port:              ${broker.port}")
        echo("Rack:              ${broker.rack ?: "N/A"}")
        echo("Version:           ${broker.version}")
        echo()
        
        if (broker.listeners.isNotEmpty()) {
            echo("Listeners:")
            broker.listeners.forEach { listener ->
                echo("  $listener")
            }
            echo()
        }
        
        if (broker.configs.isNotEmpty()) {
            echo("Configurations:")
            broker.configs.forEach { (key, value) ->
                echo("  ${key.padEnd(30)} = $value")
            }
        }
        
        if (showLogDirs && broker.logDirs.isNotEmpty()) {
            echo()
            echo("Log Directories:")
            broker.logDirs.forEach { logDir ->
                val totalGb = logDir.totalBytes / (1024 * 1024 * 1024)
                val usedGb = logDir.usedBytes / (1024 * 1024 * 1024)
                val availableGb = logDir.availableBytes / (1024 * 1024 * 1024)
                echo("  ${logDir.path}:")
                echo("    Total:     $totalGb GB")
                echo("    Used:      $usedGb GB (${"%.1f".format(logDir.usedPercent)}%)")
                echo("    Available: $availableGb GB")
            }
        }
    }
}
