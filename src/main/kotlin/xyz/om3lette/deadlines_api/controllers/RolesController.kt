package xyz.om3lette.deadlines_api.controllers

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.roles.request.ChangeRoleRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.RolesService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/role")
class RolesController(
    private val rolesService: RolesService
) {
    @PostMapping("/organization/{organizationId}")
    fun changeOrganizationRole(
        @AuthenticationPrincipal issuer: User,
        @PathVariable organizationId: Long,
        @Valid @RequestBody request: ChangeRoleRequest
    ) = rolesService.changeRole(issuer, organizationId, request.subjectUsername, request.newRole, "ORG")

    @PostMapping("/thread/{threadId}")
    fun changeThreadRole(
        @AuthenticationPrincipal issuer: User,
        @PathVariable threadId: Long,
        @Valid @RequestBody request: ChangeRoleRequest
    ) = rolesService.changeRole(issuer, threadId, request.subjectUsername, request.newRole, "THR")

    @PostMapping("/deadline/{deadlineId}")
    fun changeDeadlineRole(
        @AuthenticationPrincipal issuer: User,
        @PathVariable deadlineId: Long,
        @Valid @RequestBody request: ChangeRoleRequest
    ) = rolesService.changeRole(issuer, deadlineId, request.subjectUsername, request.newRole, "DDL")
}