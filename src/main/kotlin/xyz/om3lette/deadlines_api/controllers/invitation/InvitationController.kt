package xyz.om3lette.deadlines_api.controllers.invitation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.OrganizationInvitationService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/invitation")
@Tag(name = "Invitations")
class InvitationController(
    private val organizationInvitationService: OrganizationInvitationService
) {
    @GetMapping("/{invitationId}")
    @Operation(summary = "Get invitation details")
    fun getInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable invitationId: Long
    ) = organizationInvitationService.getInvitation(user, invitationId)

    @PostMapping("/{invitationId}/revoke")
    @Operation(summary = "Revoke invitation")
    fun revokeInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable invitationId: Long
    ) = organizationInvitationService.revokeInvitation(user, invitationId)

    @PostMapping("/{invitationId}/accept")
    @Operation(
        summary = "Accept an invitation",
        description = "Upon accepting user is added to the organization with a role specified in the invitation."
    )
    fun acceptInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable invitationId: Long
    ) = organizationInvitationService.resolveInvitation(
        user,
        invitationId,
        InvitationStatus.ACCEPTED
    )

    @PostMapping("/{invitationId}/decline")
    @Operation(summary = "Decline invitation")
    fun declineInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable invitationId: Long
    ) = organizationInvitationService.resolveInvitation(
        user,
        invitationId,
        InvitationStatus.DECLINED
    )

    @GetMapping("/me/pending-received-number")
    @Operation(summary = "Get number of pending invitations which were sent to the user")
    fun getNumberOfPendingInvitation(
        @AuthenticationPrincipal user: User,
    ) = organizationInvitationService.getNumberOfPendingInvitationsByUser(user)

    @GetMapping("/me/pending-received")
    @Operation(summary = "Get pending invitations which were sent to the user")
    fun getPendingInvitationsByUser(
        @AuthenticationPrincipal user: User,
        @RequestParam("page") pageNumber: Int
    ) = organizationInvitationService.getPendingInvitationsByUser(user, pageNumber, 10)

    @GetMapping("/me/pending-sent")
    @Operation(summary = "Get pending invitations which were sent by the user")
    fun getPendingSentInvitationsByUser(
        @AuthenticationPrincipal user: User,
        @RequestParam("page") pageNumber: Int
    ) = organizationInvitationService.getPendingSentInvitationsByUser(user, pageNumber, 10)
}
