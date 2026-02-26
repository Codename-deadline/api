package xyz.om3lette.deadlines_api.data.scopes.organization.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationStats

data class OrganizationResponse(
    @get:JsonUnwrapped
    val organization: OrganizationDTO,
    val stats: OrganizationStats,
)
