package xyz.om3lette.deadlines_api.controllers.organization

import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
class OrganizationInvitationController(
    private val organizationInvitationService: OrganizationInvitationService
) {
    @PostMapping
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
    fun getInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable invitationId: Long
    ) = organizationInvitationService.getInvitation(user, invitationId)

    @PostMapping("/{invitationId}/accept")
    fun acceptInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable invitationId: Long
    ) = organizationInvitationService.resolveInvitation(
        user,
        invitationId,
        InvitationStatus.ACCEPTED
    )

    @PostMapping("/{invitationId}/decline")
    fun declineInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable invitationId: Long
    ) = organizationInvitationService.resolveInvitation(
        user,
        invitationId,
        InvitationStatus.DECLINED
    )
}