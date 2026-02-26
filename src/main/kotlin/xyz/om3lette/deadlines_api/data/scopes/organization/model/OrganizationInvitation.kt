package xyz.om3lette.deadlines_api.data.scopes.organization.model

import jakarta.persistence.*
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationInvitationResponse
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import java.time.Instant

@Entity
@Table(name = "organization_invitations")
data class OrganizationInvitation(
    @Id
    @SequenceGenerator(name = "org_inv_seq", sequenceName = "org_inv_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "org_inv_seq")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invited_by_user_id")
    val invitedBy: User,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invited_user_id")
    val invitedUser: User,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    val organization: Organization,

    @Enumerated(value = EnumType.STRING)
    var status: InvitationStatus,

    val role: ScopeRole,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val createdAt: Instant,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    var answeredAt: Instant? = null
) {
    fun toMap() = mapOf(
        "id" to id,
        "invitedBy" to invitedBy.toMap(),
        "invitedUser" to invitedUser.toMap(),
        "organization" to organization.toMap(),
        "status" to status.toString(),
        "role" to role.toString(),
        "createdAt" to createdAt,
        "answeredAt" to answeredAt
    )

    fun toResponse() = OrganizationInvitationResponse(
        id,
        invitedBy.toResponse(),
        invitedUser.toResponse(),
        organization.toResponse(),
        status.name,
        role.name,
        createdAt,
        answeredAt
    )
}
