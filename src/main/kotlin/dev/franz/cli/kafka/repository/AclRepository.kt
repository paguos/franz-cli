package dev.franz.cli.kafka.repository

import dev.franz.cli.kafka.model.Acl
import dev.franz.cli.kafka.model.AclOperation
import dev.franz.cli.kafka.model.AclPermission
import dev.franz.cli.kafka.model.PatternType
import dev.franz.cli.kafka.model.ResourceType

interface AclRepository {
    fun listAcls(
        principal: String? = null,
        resourceType: ResourceType? = null,
        resourceName: String? = null
    ): List<Acl>
    
    fun createAcl(
        principal: String,
        resourceType: ResourceType,
        resourceName: String,
        operation: AclOperation,
        permission: AclPermission,
        patternType: PatternType = PatternType.LITERAL
    ): Acl
    
    fun deleteAcls(
        principal: String? = null,
        resourceType: ResourceType? = null,
        resourceName: String? = null,
        operation: AclOperation? = null
    ): List<Acl>
}
