package xyz.om3lette.deadlines_api.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.scopes.organization.request.member.MemberInvitationRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.OrganizationInvitationService

@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/organizations/{organizationId}/invitations")
@Tag(name = "Invitations")
class OrganizationInvitationController(
    private val organizationInvitationService: OrganizationInvitationService
) {
    @PostMapping
    @Operation(summary = "Create a new organization invitation")
    fun sendInvitation(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive organizationId: Long,
        @Valid @RequestBody request: MemberInvitationRequest
    ) = organizationInvitationService.createInvitation(
        user,
        organizationId,
        request.username,
        request.role
    )
}
