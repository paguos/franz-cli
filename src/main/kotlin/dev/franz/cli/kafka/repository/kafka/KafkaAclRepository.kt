package dev.franz.cli.kafka.repository.kafka

import dev.franz.cli.kafka.model.Acl
import dev.franz.cli.kafka.model.AclOperation
import dev.franz.cli.kafka.model.AclPermission
import dev.franz.cli.kafka.model.PatternType
import dev.franz.cli.kafka.model.ResourceType
import dev.franz.cli.kafka.repository.AclRepository
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.common.acl.AccessControlEntry
import org.apache.kafka.common.acl.AccessControlEntryFilter
import org.apache.kafka.common.acl.AclBinding
import org.apache.kafka.common.acl.AclBindingFilter
import org.apache.kafka.common.resource.ResourcePattern
import org.apache.kafka.common.resource.ResourcePatternFilter

/**
 * Real Kafka implementation of AclRepository.
 * Uses Kafka AdminClient to interact with ACLs.
 */
class KafkaAclRepository(
    private val adminClient: AdminClient
) : AclRepository {
    
    override fun listAcls(principal: String?, resourceType: ResourceType?, resourceName: String?): List<Acl> {
        val resourcePatternFilter = ResourcePatternFilter(
            resourceType?.toKafkaResourceType() ?: org.apache.kafka.common.resource.ResourceType.ANY,
            resourceName,
            org.apache.kafka.common.resource.PatternType.ANY
        )
        
        val accessControlEntryFilter = AccessControlEntryFilter(
            principal,
            null,
            org.apache.kafka.common.acl.AclOperation.ANY,
            org.apache.kafka.common.acl.AclPermissionType.ANY
        )
        
        val filter = AclBindingFilter(resourcePatternFilter, accessControlEntryFilter)
        val aclBindings = adminClient.describeAcls(filter).values().get()
        
        return aclBindings.map { it.toAcl() }
    }
    
    override fun createAcl(
        principal: String,
        resourceType: ResourceType,
        resourceName: String,
        operation: AclOperation,
        permission: AclPermission,
        patternType: PatternType
    ): Acl {
        val resourcePattern = ResourcePattern(
            resourceType.toKafkaResourceType(),
            resourceName,
            patternType.toKafkaPatternType()
        )
        
        val accessControlEntry = AccessControlEntry(
            principal,
            "*",
            operation.toKafkaOperation(),
            permission.toKafkaPermission()
        )
        
        val aclBinding = AclBinding(resourcePattern, accessControlEntry)
        adminClient.createAcls(listOf(aclBinding)).all().get()
        
        return Acl(principal, resourceType, resourceName, patternType, operation, permission)
    }
    
    override fun deleteAcls(
        principal: String?,
        resourceType: ResourceType?,
        resourceName: String?,
        operation: AclOperation?
    ): List<Acl> {
        val resourcePatternFilter = ResourcePatternFilter(
            resourceType?.toKafkaResourceType() ?: org.apache.kafka.common.resource.ResourceType.ANY,
            resourceName,
            org.apache.kafka.common.resource.PatternType.ANY
        )
        
        val accessControlEntryFilter = AccessControlEntryFilter(
            principal,
            null,
            operation?.toKafkaOperation() ?: org.apache.kafka.common.acl.AclOperation.ANY,
            org.apache.kafka.common.acl.AclPermissionType.ANY
        )
        
        val filter = AclBindingFilter(resourcePatternFilter, accessControlEntryFilter)
        val deletedBindings = adminClient.deleteAcls(listOf(filter)).all().get()
        
        return deletedBindings.map { it.toAcl() }
    }
    
    // Extension functions for converting between domain and Kafka types
    
    private fun ResourceType.toKafkaResourceType(): org.apache.kafka.common.resource.ResourceType {
        return when (this) {
            ResourceType.TOPIC -> org.apache.kafka.common.resource.ResourceType.TOPIC
            ResourceType.GROUP -> org.apache.kafka.common.resource.ResourceType.GROUP
            ResourceType.CLUSTER -> org.apache.kafka.common.resource.ResourceType.CLUSTER
            ResourceType.TRANSACTIONAL_ID -> org.apache.kafka.common.resource.ResourceType.TRANSACTIONAL_ID
        }
    }
    
    private fun PatternType.toKafkaPatternType(): org.apache.kafka.common.resource.PatternType {
        return when (this) {
            PatternType.LITERAL -> org.apache.kafka.common.resource.PatternType.LITERAL
            PatternType.PREFIXED -> org.apache.kafka.common.resource.PatternType.PREFIXED
        }
    }
    
    private fun AclOperation.toKafkaOperation(): org.apache.kafka.common.acl.AclOperation {
        return when (this) {
            AclOperation.READ -> org.apache.kafka.common.acl.AclOperation.READ
            AclOperation.WRITE -> org.apache.kafka.common.acl.AclOperation.WRITE
            AclOperation.CREATE -> org.apache.kafka.common.acl.AclOperation.CREATE
            AclOperation.DELETE -> org.apache.kafka.common.acl.AclOperation.DELETE
            AclOperation.ALTER -> org.apache.kafka.common.acl.AclOperation.ALTER
            AclOperation.DESCRIBE -> org.apache.kafka.common.acl.AclOperation.DESCRIBE
            AclOperation.ALL -> org.apache.kafka.common.acl.AclOperation.ALL
        }
    }
    
    private fun AclPermission.toKafkaPermission(): org.apache.kafka.common.acl.AclPermissionType {
        return when (this) {
            AclPermission.ALLOW -> org.apache.kafka.common.acl.AclPermissionType.ALLOW
            AclPermission.DENY -> org.apache.kafka.common.acl.AclPermissionType.DENY
        }
    }
    
    private fun AclBinding.toAcl(): Acl {
        return Acl(
            principal = entry().principal(),
            resourceType = pattern().resourceType().toDomainResourceType(),
            resourceName = pattern().name(),
            patternType = pattern().patternType().toDomainPatternType(),
            operation = entry().operation().toDomainOperation(),
            permission = entry().permissionType().toDomainPermission()
        )
    }
    
    private fun org.apache.kafka.common.resource.ResourceType.toDomainResourceType(): ResourceType {
        return when (this) {
            org.apache.kafka.common.resource.ResourceType.TOPIC -> ResourceType.TOPIC
            org.apache.kafka.common.resource.ResourceType.GROUP -> ResourceType.GROUP
            org.apache.kafka.common.resource.ResourceType.CLUSTER -> ResourceType.CLUSTER
            org.apache.kafka.common.resource.ResourceType.TRANSACTIONAL_ID -> ResourceType.TRANSACTIONAL_ID
            else -> ResourceType.TOPIC // Default fallback
        }
    }
    
    private fun org.apache.kafka.common.resource.PatternType.toDomainPatternType(): PatternType {
        return when (this) {
            org.apache.kafka.common.resource.PatternType.PREFIXED -> PatternType.PREFIXED
            else -> PatternType.LITERAL
        }
    }
    
    private fun org.apache.kafka.common.acl.AclOperation.toDomainOperation(): AclOperation {
        return when (this) {
            org.apache.kafka.common.acl.AclOperation.READ -> AclOperation.READ
            org.apache.kafka.common.acl.AclOperation.WRITE -> AclOperation.WRITE
            org.apache.kafka.common.acl.AclOperation.CREATE -> AclOperation.CREATE
            org.apache.kafka.common.acl.AclOperation.DELETE -> AclOperation.DELETE
            org.apache.kafka.common.acl.AclOperation.ALTER -> AclOperation.ALTER
            org.apache.kafka.common.acl.AclOperation.DESCRIBE -> AclOperation.DESCRIBE
            org.apache.kafka.common.acl.AclOperation.ALL -> AclOperation.ALL
            else -> AclOperation.ALL // Default fallback
        }
    }
    
    private fun org.apache.kafka.common.acl.AclPermissionType.toDomainPermission(): AclPermission {
        return when (this) {
            org.apache.kafka.common.acl.AclPermissionType.DENY -> AclPermission.DENY
            else -> AclPermission.ALLOW
        }
    }
}
