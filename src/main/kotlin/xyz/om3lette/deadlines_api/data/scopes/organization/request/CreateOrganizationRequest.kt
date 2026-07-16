package xyz.om3lette.deadlines_api.data.scopes.organization.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeTextConstraints
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeInvitationConstraints
import xyz.om3lette.deadlines_api.data.scopes.common.dto.UsernameRolePair
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType

data class CreateOrganizationRequest(
    @field:NotBlank
    @field:Size(min = ScopeTextConstraints.TITLE_MIN, max = ScopeTextConstraints.TITLE_MAX)
    val title: String,

    @field:Size(max = ScopeTextConstraints.DESCRIPTION_MAX)
    val description: String?,

    @field:NotNull
    val type: OrganizationType,

    @field:Valid
    @field:Size(max = ScopeInvitationConstraints.MAX_INVITATIONS)
    val invitations: List<@Valid UsernameRolePair>
)
