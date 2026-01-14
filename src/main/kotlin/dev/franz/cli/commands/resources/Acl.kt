package dev.franz.cli.commands.resources

import dev.franz.cli.FranzCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import dev.franz.cli.kafka.KafkaService
import dev.franz.cli.kafka.model.AclOperation
import dev.franz.cli.kafka.model.AclPermission
import dev.franz.cli.kafka.model.PatternType
import dev.franz.cli.kafka.model.ResourceType

class GetAcl : FranzCommand(
    name = "acl",
    help = """
        List Kafka ACLs.

        Examples:
        ```
        franz get acl
        franz get acl --principal User:alice
        franz get acl --resource-type topic --resource-name my-topic
        ```
    """.trimIndent()
) {
    private val principal by option("--principal", "-p", help = "Filter by principal")
    private val resourceType by option("--resource-type", "-r", help = "Filter by resource type")
        .choice("topic", "group", "cluster", "transactional-id")
    private val resourceName by option("--resource-name", "-n", help = "Filter by resource name")

    override fun run() {
        val kafka = KafkaService.getInstance()
        val resType = resourceType?.let { parseResourceType(it) }
        val acls = kafka.acls.listAcls(principal, resType, resourceName)

        output.table(
            headers = listOf("PRINCIPAL", "RESOURCE_TYPE", "RESOURCE_NAME", "OPERATION", "PERMISSION"),
            rows = acls.map { acl ->
                listOf(
                    acl.principal,
                    acl.resourceType.name,
                    acl.resourceName,
                    acl.operation.name,
                    acl.permission.name
                )
            }
        )
    }
    
    private fun parseResourceType(type: String): ResourceType = when (type) {
        "topic" -> ResourceType.TOPIC
        "group" -> ResourceType.GROUP
        "cluster" -> ResourceType.CLUSTER
        "transactional-id" -> ResourceType.TRANSACTIONAL_ID
        else -> ResourceType.TOPIC
    }
}

class CreateAcl : FranzCommand(
    name = "acl",
    help = """
        Create a Kafka ACL.

        Examples:
        ```
        franz create acl --principal User:alice --resource-type topic --resource-name my-topic --operation Read --permission Allow
        ```
    """.trimIndent()
) {
    private val principal by option("--principal", "-p", help = "Principal (e.g., User:alice)").required()
    private val resourceType by option("--resource-type", "-r", help = "Resource type")
        .choice("topic", "group", "cluster", "transactional-id")
        .default("topic")
    private val resourceName by option("--resource-name", "-n", help = "Resource name (use * for all)").required()
    private val operation by option("--operation", "-o", help = "Operation")
        .choice("Read", "Write", "Create", "Delete", "Alter", "Describe", "All")
        .default("Read")
    private val permission by option("--permission", help = "Permission type")
        .choice("Allow", "Deny")
        .default("Allow")
    private val patternType by option("--pattern-type", help = "Resource pattern type")
        .choice("literal", "prefixed")
        .default("literal")

    override fun run() {
        val kafka = KafkaService.getInstance()
        val resType = parseResourceType(resourceType)
        val op = AclOperation.valueOf(operation.uppercase())
        val perm = AclPermission.valueOf(permission.uppercase())
        val pattern = if (patternType == "literal") PatternType.LITERAL else PatternType.PREFIXED
        
        val acl = kafka.acls.createAcl(principal, resType, resourceName, op, perm, pattern)

        output.kvTable(
            listOf(
                "Principal" to acl.principal,
                "Resource Type" to acl.resourceType.name.lowercase(),
                "Resource Name" to acl.resourceName,
                "Pattern Type" to acl.patternType.name.lowercase(),
                "Operation" to acl.operation.name,
                "Permission" to acl.permission.name
            )
        )
        output.line()
        output.line("ACL created successfully.")
    }
    
    private fun parseResourceType(type: String): ResourceType = when (type) {
        "topic" -> ResourceType.TOPIC
        "group" -> ResourceType.GROUP
        "cluster" -> ResourceType.CLUSTER
        "transactional-id" -> ResourceType.TRANSACTIONAL_ID
        else -> ResourceType.TOPIC
    }
}

class DeleteAcl : FranzCommand(
    name = "acl",
    help = """
        Delete Kafka ACLs.

        Examples:
        ```
        franz delete acl --principal User:alice --resource-type topic --resource-name my-topic --operation Read
        franz delete acl --principal User:alice --resource-type topic --resource-name my-topic --operation Read --force
        ```
    """.trimIndent()
) {
    private val principal by option("--principal", "-p", help = "Principal to match")
    private val resourceType by option("--resource-type", "-r", help = "Resource type to match")
        .choice("topic", "group", "cluster", "transactional-id")
    private val resourceName by option("--resource-name", "-n", help = "Resource name to match")
    private val operation by option("--operation", "-o", help = "Operation to match")
        .choice("Read", "Write", "Create", "Delete", "Alter", "Describe", "All")
    private val force by option("--force", "-f", help = "Skip confirmation").flag()

    override fun run() {
        val kafka = KafkaService.getInstance()
        val resType = resourceType?.let { parseResourceType(it) }
        val op = operation?.let { AclOperation.valueOf(it.uppercase()) }
        
        // Preview what would be deleted
        val matching = kafka.acls.listAcls(principal, resType, resourceName)
            .filter { op == null || it.operation == op }

        output.line("Found ${matching.size} matching ACLs.")
        if (matching.isNotEmpty()) {
            output.line()
            output.table(
                headers = listOf("PRINCIPAL", "RESOURCE_TYPE", "RESOURCE_NAME", "OPERATION", "PERMISSION"),
                rows = matching.map { acl ->
                    listOf(
                        acl.principal,
                        acl.resourceType.name,
                        acl.resourceName,
                        acl.operation.name,
                        acl.permission.name
                    )
                }
            )
            output.line()
        }
        
        if (force) {
            val deleted = kafka.acls.deleteAcls(principal, resType, resourceName, op)
            output.line("Deleted ${deleted.size} ACLs.")
        } else {
            output.line("Use --force to confirm deletion.")
        }
    }
    
    private fun parseResourceType(type: String): ResourceType = when (type) {
        "topic" -> ResourceType.TOPIC
        "group" -> ResourceType.GROUP
        "cluster" -> ResourceType.CLUSTER
        "transactional-id" -> ResourceType.TRANSACTIONAL_ID
        else -> ResourceType.TOPIC
    }
}
