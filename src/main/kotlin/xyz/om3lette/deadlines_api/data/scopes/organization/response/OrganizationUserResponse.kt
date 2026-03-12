package xyz.om3lette.deadlines_api.data.scopes.organization.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationDTO

data class OrganizationUserResponse (
    @get:JsonUnwrapped
    val organization: OrganizationDTO,
    val stats: OrganizationStats,
)
