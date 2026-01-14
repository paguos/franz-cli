package dev.franz.cli.commands.resources

import dev.franz.cli.FranzCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.franz.cli.kafka.KafkaService
import java.text.NumberFormat

class GetGroup : FranzCommand(
    name = "group",
    help = """
        List Kafka consumer groups.

        Examples:
        ```
        franz get group
        franz get group payments-*
        franz get group --show-empty
        ```
    """.trimIndent()
) {
    private val pattern by argument(help = "Optional group name pattern to filter").optional()
    private val showEmpty by option("--show-empty", "-e", help = "Include empty groups").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        val groups = kafka.groups.listGroups(includeEmpty = showEmpty, pattern = pattern)

        output.table(
            headers = listOf("GROUP", "STATE", "MEMBERS"),
            rows = groups.map { group ->
                listOf(
                    group.name,
                    group.state,
                    group.members.size.toString()
                )
            }
        )
    }
}

class DescribeGroup : FranzCommand(
    name = "group",
    help = """
        Show detailed information about a Kafka consumer group.

        Examples:
        ```
        franz describe group my-group
        franz describe group my-group --members
        ```
    """.trimIndent()
) {
    private val name by argument(help = "Consumer group name")
    private val showMembers by option("--members", "-m", help = "Show member details").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        val group = kafka.groups.describeGroup(name)
        
        if (group == null) {
            errorLine("Consumer group '$name' not found.")
            return
        }

        output.kvTable(
            listOf(
                "Name" to group.name,
                "State" to group.state,
                "Protocol Type" to group.protocolType,
                "Protocol" to group.protocol,
                "Coordinator" to group.coordinator
            )
        )

        if (group.topicSubscriptions.isNotEmpty()) {
            output.line()
            output.section("Topic Subscriptions")
            output.table(
                headers = listOf("TOPIC", "PARTITIONS"),
                rows = group.topicSubscriptions.map { sub ->
                    listOf(sub.topic, sub.partitions.toString())
                }
            )
        }

        output.line()
        output.section("Lag Summary")
        output.kvTable(
            listOf(
                "Total Lag" to "${NumberFormat.getInstance().format(group.totalLag)} messages"
            )
        )

        if (showMembers && group.members.isNotEmpty()) {
            output.line()
            output.section("Members")
            output.table(
                headers = listOf("MEMBER_ID", "CLIENT_ID", "ASSIGNMENTS"),
                rows = group.members.map { member ->
                    listOf(
                        member.memberId,
                        member.clientId,
                        member.assignments.joinToString(", ")
                    )
                }
            )
        }
    }
}
