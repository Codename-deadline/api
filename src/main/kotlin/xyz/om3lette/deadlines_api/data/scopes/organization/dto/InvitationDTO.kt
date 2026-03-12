package xyz.om3lette.deadlines_api.data.scopes.organization.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationInvitationRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class InvitationDTO(
    @field:NotBlank val username: String,
    @field:NotNull val role: OrganizationInvitationRole
) {
    fun toScopeRole() =
        when (this.role) {
            OrganizationInvitationRole.ORG_MEMBER -> ScopeRole.ORG_MEMBER
            OrganizationInvitationRole.ORG_ADMIN -> ScopeRole.ORG_ADMIN
        }
}