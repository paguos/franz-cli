package dev.franz.cli.commands.resources

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class GetBroker : CliktCommand(
    name = "broker",
    help = "List Kafka brokers"
) {
    private val showConfigs by option("--configs", "-c", help = "Show broker configurations").flag()

    override fun run() {
        echo("Listing brokers...")
        echo()
        echo("ID    HOST                PORT    RACK")
        echo("1     broker-1.kafka      9092    us-east-1a")
        echo("2     broker-2.kafka      9092    us-east-1b")
        echo("3     broker-3.kafka      9092    us-east-1c")
        echo()
        echo("Controller: broker-1.kafka:9092 (id: 1)")
        
        if (showConfigs) {
            echo()
            echo("Common Broker Configs:")
            echo("  log.retention.hours=168")
            echo("  num.partitions=3")
            echo("  default.replication.factor=2")
        }
    }
}

class DescribeBroker : CliktCommand(
    name = "broker",
    help = "Show detailed information about a broker"
) {
    private val id by argument(help = "Broker ID")
    private val showLogDirs by option("--log-dirs", "-l", help = "Show log directory details").flag()

    override fun run() {
        echo("Broker: $id")
        echo("=".repeat(50))
        echo("Host:              broker-$id.kafka")
        echo("Port:              9092")
        echo("Rack:              us-east-1a")
        echo("Version:           3.6.0")
        echo()
        echo("Listeners:")
        echo("  PLAINTEXT://broker-$id.kafka:9092")
        echo("  SSL://broker-$id.kafka:9093")
        echo()
        echo("Configurations:")
        echo("  log.retention.hours          = 168")
        echo("  log.segment.bytes            = 1073741824")
        echo("  num.io.threads               = 8")
        echo("  num.network.threads          = 3")
        echo("  num.replica.fetchers         = 1")
        
        if (showLogDirs) {
            echo()
            echo("Log Directories:")
            echo("  /var/kafka/data-1:")
            echo("    Total:     500 GB")
            echo("    Used:      234 GB (46.8%)")
            echo("    Available: 266 GB")
            echo("  /var/kafka/data-2:")
            echo("    Total:     500 GB")
            echo("    Used:      198 GB (39.6%)")
            echo("    Available: 302 GB")
        }
    }
}
