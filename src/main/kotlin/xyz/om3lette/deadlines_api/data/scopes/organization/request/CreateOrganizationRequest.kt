package xyz.om3lette.deadlines_api.data.scopes.organization.request

import xyz.om3lette.deadlines_api.data.scopes.common.dto.UsernameRolePairList
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType

data class CreateOrganizationRequest(
    val title: String,

    val description: String?,

    val type: OrganizationType,

    val invitations: UsernameRolePairList
)
