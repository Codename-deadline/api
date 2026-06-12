package xyz.om3lette.deadlines_api.data.scopes.thread.dto

import java.time.Instant

data class ThreadDTO(
    val id: Long,

    val title: String,

    val description: String?,

    val organizationId: Long,

    val createdAt: Instant
)
