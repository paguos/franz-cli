package dev.franz.cli.kafka.repository

import dev.franz.cli.kafka.model.ConsumerGroup

interface GroupRepository {
    fun listGroups(includeEmpty: Boolean = false, pattern: String? = null): List<ConsumerGroup>
    fun describeGroup(name: String): ConsumerGroup?
    fun deleteGroup(name: String): Boolean
}
