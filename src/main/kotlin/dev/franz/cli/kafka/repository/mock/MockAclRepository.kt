package dev.franz.cli.kafka.repository.mock

import dev.franz.cli.kafka.model.Acl
import dev.franz.cli.kafka.model.AclOperation
import dev.franz.cli.kafka.model.AclPermission
import dev.franz.cli.kafka.model.PatternType
import dev.franz.cli.kafka.model.ResourceType
import dev.franz.cli.kafka.repository.AclRepository

class MockAclRepository : AclRepository {
    
    private val acls = mutableListOf(
        Acl("User:admin", ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL, AclOperation.ALL, AclPermission.ALLOW),
        Acl("User:producer-app", ResourceType.TOPIC, "events", PatternType.LITERAL, AclOperation.WRITE, AclPermission.ALLOW),
        Acl("User:producer-app", ResourceType.TOPIC, "events", PatternType.LITERAL, AclOperation.DESCRIBE, AclPermission.ALLOW),
        Acl("User:consumer-app", ResourceType.TOPIC, "events", PatternType.LITERAL, AclOperation.READ, AclPermission.ALLOW),
        Acl("User:consumer-app", ResourceType.GROUP, "consumer-group-1", PatternType.LITERAL, AclOperation.READ, AclPermission.ALLOW),
        Acl("User:analytics", ResourceType.TOPIC, "*", PatternType.LITERAL, AclOperation.READ, AclPermission.ALLOW)
    )
    
    override fun listAcls(principal: String?, resourceType: ResourceType?, resourceName: String?): List<Acl> {
        return acls
            .filter { principal == null || it.principal == principal }
            .filter { resourceType == null || it.resourceType == resourceType }
            .filter { resourceName == null || it.resourceName == resourceName }
    }
    
    override fun createAcl(
        principal: String,
        resourceType: ResourceType,
        resourceName: String,
        operation: AclOperation,
        permission: AclPermission,
        patternType: PatternType
    ): Acl {
        val acl = Acl(principal, resourceType, resourceName, patternType, operation, permission)
        acls.add(acl)
        return acl
    }
    
    override fun deleteAcls(
        principal: String?,
        resourceType: ResourceType?,
        resourceName: String?,
        operation: AclOperation?
    ): List<Acl> {
        val toDelete = acls.filter { acl ->
            (principal == null || acl.principal == principal) &&
            (resourceType == null || acl.resourceType == resourceType) &&
            (resourceName == null || acl.resourceName == resourceName) &&
            (operation == null || acl.operation == operation)
        }
        acls.removeAll(toDelete)
        return toDelete
    }
}
