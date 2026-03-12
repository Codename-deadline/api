package xyz.om3lette.deadlines_api.data.scopes.organization.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class OrganizationResponseWithRole (
    @get:JsonUnwrapped
    val response: OrganizationResponse,
    val role: ScopeRole
)
