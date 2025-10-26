package xyz.om3lette.deadlines_api.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.request.member.MemberInvitationRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.OrganizationInvitationService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/organization/{organizationId}/invitation")
@Tag(name = "Invitations")
class OrganizationInvitationController(
    private val organizationInvitationService: OrganizationInvitationService
) {
    @PostMapping
    @Operation(summary = "Create a new organization invitation")
    fun sendInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable organizationId: Long,
        @RequestBody request: MemberInvitationRequest
    ) = organizationInvitationService.createInvitation(
        user,
        organizationId,
        request.username,
        request.role
    )

    @GetMapping("/{invitationId}")
    @Operation(summary = "Get invitation details")
    fun getInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable invitationId: Long
    ) = organizationInvitationService.getInvitation(user, invitationId)

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
}