package xyz.om3lette.deadlines_api.data.scopes.thread.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeTextConstraints
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeInvitationConstraints
import xyz.om3lette.deadlines_api.data.scopes.common.dto.UsernameRolePair

data class CreateThreadRequest(
    @field:NotBlank
    @field:Size(min = ScopeTextConstraints.TITLE_MIN, max = ScopeTextConstraints.TITLE_MAX)
    val title: String,

    @field:Size(max = ScopeTextConstraints.DESCRIPTION_MAX)
    val description: String?,

    @field:Valid
    @field:Size(max = ScopeInvitationConstraints.MAX_INVITATIONS)
    val invitations: List<@Valid UsernameRolePair>
)
