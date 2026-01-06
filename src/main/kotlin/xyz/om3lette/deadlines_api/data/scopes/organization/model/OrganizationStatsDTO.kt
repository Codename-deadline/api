package xyz.om3lette.deadlines_api.data.scopes.organization.model

import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationStatsResponse

data class OrganizationStatsDTO(
    val members: Int,

    val threads: Int
) {
    fun toResponse(): OrganizationStatsResponse = OrganizationStatsResponse(
        members = members,
        threads = threads
    )
}