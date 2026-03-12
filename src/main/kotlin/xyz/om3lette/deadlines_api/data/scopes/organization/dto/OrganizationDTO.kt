package xyz.om3lette.deadlines_api.data.scopes.organization.dto

import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import java.time.Instant

data class OrganizationDTO(
    val id: Long,

    val title: String,

    val description: String?,

    val type: OrganizationType,

    val createdAt: Instant
)