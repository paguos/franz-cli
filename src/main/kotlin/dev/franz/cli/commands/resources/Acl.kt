package dev.franz.cli.commands.resources

import com.github.ajalt.clikt.core.CliktCommand
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

class GetAcl : CliktCommand(
    name = "acl",
    help = "List Kafka ACLs"
) {
    private val principal by option("--principal", "-p", help = "Filter by principal")
    private val resourceType by option("--resource-type", "-r", help = "Filter by resource type")
        .choice("topic", "group", "cluster", "transactional-id")
    private val resourceName by option("--resource-name", "-n", help = "Filter by resource name")

    override fun run() {
        val kafka = KafkaService.getInstance()
        echo("Listing ACLs...")
        if (principal != null) echo("  Principal: $principal")
        if (resourceType != null) echo("  Resource Type: $resourceType")
        if (resourceName != null) echo("  Resource Name: $resourceName")
        echo()
        
        val resType = resourceType?.let { parseResourceType(it) }
        val acls = kafka.acls.listAcls(principal, resType, resourceName)
        
        echo("PRINCIPAL                  RESOURCE TYPE   RESOURCE NAME        OPERATION   PERMISSION")
        acls.forEach { acl ->
            echo("${acl.principal.padEnd(26)} ${acl.resourceType.name.padEnd(15)} ${acl.resourceName.padEnd(20)} ${acl.operation.name.padEnd(11)} ${acl.permission.name}")
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

class CreateAcl : CliktCommand(
    name = "acl",
    help = "Create a Kafka ACL"
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
        
        echo("Creating ACL...")
        echo()
        echo("  Principal:      ${acl.principal}")
        echo("  Resource Type:  ${acl.resourceType.name.lowercase()}")
        echo("  Resource Name:  ${acl.resourceName}")
        echo("  Pattern Type:   ${acl.patternType.name.lowercase()}")
        echo("  Operation:      ${acl.operation.name}")
        echo("  Permission:     ${acl.permission.name}")
        echo()
        echo("ACL created successfully.")
    }
    
    private fun parseResourceType(type: String): ResourceType = when (type) {
        "topic" -> ResourceType.TOPIC
        "group" -> ResourceType.GROUP
        "cluster" -> ResourceType.CLUSTER
        "transactional-id" -> ResourceType.TRANSACTIONAL_ID
        else -> ResourceType.TOPIC
    }
}

class DeleteAcl : CliktCommand(
    name = "acl",
    help = "Delete Kafka ACLs"
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
        echo("Matching ACLs to delete...")
        if (principal != null) echo("  Principal: $principal")
        if (resourceType != null) echo("  Resource Type: $resourceType")
        if (resourceName != null) echo("  Resource Name: $resourceName")
        if (operation != null) echo("  Operation: $operation")
        echo()
        
        val resType = resourceType?.let { parseResourceType(it) }
        val op = operation?.let { AclOperation.valueOf(it.uppercase()) }
        
        // Preview what would be deleted
        val matching = kafka.acls.listAcls(principal, resType, resourceName)
            .filter { op == null || it.operation == op }
        
        echo("Found ${matching.size} matching ACLs:")
        matching.forEach { acl ->
            echo("  ${acl.principal}  ${acl.resourceType.name}  ${acl.resourceName}  ${acl.operation.name}  ${acl.permission.name}")
        }
        echo()
        
        if (force) {
            val deleted = kafka.acls.deleteAcls(principal, resType, resourceName, op)
            echo("Deleted ${deleted.size} ACLs.")
        } else {
            echo("Use --force to confirm deletion.")
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
