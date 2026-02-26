package xyz.om3lette.deadlines_api.data.scopes.organization.dto

import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationStats

data class OrganizationStatsDTO(
    val organizationId: Long,
    val members: Long,
    val threads: Long
) {
    fun toResponse(): OrganizationStats = OrganizationStats(
        members, threads
    )
}
