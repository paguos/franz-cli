package dev.franz.cli.commands.resources

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice

class GetAcl : CliktCommand(
    name = "acl",
    help = "List Kafka ACLs"
) {
    private val principal by option("--principal", "-p", help = "Filter by principal")
    private val resourceType by option("--resource-type", "-r", help = "Filter by resource type")
        .choice("topic", "group", "cluster", "transactional-id")
    private val resourceName by option("--resource-name", "-n", help = "Filter by resource name")

    override fun run() {
        echo("Listing ACLs...")
        if (principal != null) echo("  Principal: $principal")
        if (resourceType != null) echo("  Resource Type: $resourceType")
        if (resourceName != null) echo("  Resource Name: $resourceName")
        echo()
        echo("PRINCIPAL                  RESOURCE TYPE   RESOURCE NAME        OPERATION   PERMISSION")
        echo("User:admin                 Cluster         kafka-cluster        All         Allow")
        echo("User:producer-app          Topic           events               Write       Allow")
        echo("User:producer-app          Topic           events               Describe    Allow")
        echo("User:consumer-app          Topic           events               Read        Allow")
        echo("User:consumer-app          Group           consumer-group-1     Read        Allow")
        echo("User:analytics             Topic           *                    Read        Allow")
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
        echo("Creating ACL...")
        echo()
        echo("  Principal:      $principal")
        echo("  Resource Type:  $resourceType")
        echo("  Resource Name:  $resourceName")
        echo("  Pattern Type:   $patternType")
        echo("  Operation:      $operation")
        echo("  Permission:     $permission")
        echo()
        echo("ACL created successfully.")
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
        echo("Matching ACLs to delete...")
        if (principal != null) echo("  Principal: $principal")
        if (resourceType != null) echo("  Resource Type: $resourceType")
        if (resourceName != null) echo("  Resource Name: $resourceName")
        if (operation != null) echo("  Operation: $operation")
        echo()
        echo("Found 2 matching ACLs:")
        echo("  User:producer-app  Topic  events  Write  Allow")
        echo("  User:producer-app  Topic  events  Describe  Allow")
        echo()
        if (force) {
            echo("Deleted 2 ACLs.")
        } else {
            echo("Use --force to confirm deletion.")
        }
    }
}
