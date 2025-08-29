package xyz.om3lette.deadlines_api.data.scopes.organization.request.member

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class MemberInvitationRequest(
    val username: String,
    val role: ScopeRole
)