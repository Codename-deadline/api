package xyz.om3lette.deadlines_api.data.scopes.organization.request

import jakarta.validation.Valid
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.InvitationDTO

data class CreateOrganizationRequest(
    val title: String,

    val description: String?,

    val type: OrganizationType,

    val invitations: List<@Valid InvitationDTO>
)
