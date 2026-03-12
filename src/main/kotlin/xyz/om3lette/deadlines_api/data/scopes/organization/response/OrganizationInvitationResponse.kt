package xyz.om3lette.deadlines_api.data.scopes.organization.response

import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationDTO
import xyz.om3lette.deadlines_api.data.user.response.UserResponse
import java.time.Instant

data class OrganizationInvitationResponse(
    val id: Long,

    val invitedBy: UserResponse,

    val invitedUser: UserResponse,

    val organization: OrganizationDTO,

    val status: String,

    val role: String,

    val createdAt: Instant,

    val answeredAt: Instant?
)
