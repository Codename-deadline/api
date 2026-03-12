package xyz.om3lette.deadlines_api.data.scopes.organization.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationPermissions
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationStats
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class OrganizationResponse(
    @get:JsonUnwrapped
    val organization: OrganizationDTO,
    val stats: OrganizationStats,
    val permissions: OrganizationPermissions
) {
    fun withRole(scopeRole: ScopeRole): OrganizationResponseWithRole = OrganizationResponseWithRole(
        this, scopeRole
    )
}
