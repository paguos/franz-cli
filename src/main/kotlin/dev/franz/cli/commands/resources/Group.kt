package dev.franz.cli.commands.resources

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.franz.cli.kafka.KafkaService
import java.text.NumberFormat

class GetGroup : CliktCommand(
    name = "group",
    help = "List consumer groups"
) {
    private val pattern by argument(help = "Optional group name pattern to filter").optional()
    private val showEmpty by option("--show-empty", "-e", help = "Include empty groups").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        echo("Listing consumer groups...")
        if (pattern != null) {
            echo("  Filter pattern: $pattern")
        }
        echo()
        
        val groups = kafka.groups.listGroups(includeEmpty = showEmpty, pattern = pattern)
        
        echo("GROUP                         STATE            MEMBERS")
        groups.forEach { group ->
            echo("${group.name.padEnd(29)} ${group.state.padEnd(16)} ${group.members.size}")
        }
        
        if (!showEmpty) {
            echo()
            echo("(Use --show-empty to include empty groups)")
        }
    }
}

class DescribeGroup : CliktCommand(
    name = "group",
    help = "Show detailed information about a consumer group"
) {
    private val name by argument(help = "Consumer group name")
    private val showMembers by option("--members", "-m", help = "Show member details").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        val group = kafka.groups.describeGroup(name)
        
        if (group == null) {
            echo("Consumer group '$name' not found.", err = true)
            return
        }
        
        echo("Consumer Group: ${group.name}")
        echo("=".repeat(50))
        echo("State:             ${group.state}")
        echo("Protocol Type:     ${group.protocolType}")
        echo("Protocol:          ${group.protocol}")
        echo("Coordinator:       ${group.coordinator}")
        echo()
        
        if (group.topicSubscriptions.isNotEmpty()) {
            echo("Topic Subscriptions:")
            group.topicSubscriptions.forEach { sub ->
                echo("  - ${sub.topic} (${sub.partitions} partitions)")
            }
            echo()
        }
        
        echo("Lag Summary:")
        echo("  Total Lag:       ${NumberFormat.getInstance().format(group.totalLag)} messages")
        
        if (showMembers && group.members.isNotEmpty()) {
            echo()
            echo("Members:")
            group.members.forEach { member ->
                echo("  ${member.memberId} (client-id: ${member.clientId})")
                if (member.assignments.isNotEmpty()) {
                    echo("    Assigned: ${member.assignments.joinToString(", ")}")
                }
            }
        } else if (!showMembers) {
            echo()
            echo("(Use --members to show member details)")
        }
    }
}
