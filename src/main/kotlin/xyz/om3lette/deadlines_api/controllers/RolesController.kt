package xyz.om3lette.deadlines_api.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.roles.request.ChangeRoleRequest
import xyz.om3lette.deadlines_api.data.roles.response.RolesMetadataResponse
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.RolesService

@RestController
@RequestMapping("/api/role")
@Tag(name = "Roles", description = "Role management in organization / thread / deadline")
class RolesController(
    private val rolesService: RolesService
) {
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/organization/{organizationId}")
    @Operation(summary = "Assign an organization role to user")
    fun changeOrganizationRole(
        @AuthenticationPrincipal issuer: User,
        @PathVariable organizationId: Long,
        @Valid @RequestBody request: ChangeRoleRequest
    ) = rolesService.changeRole(
        issuer, organizationId, request.subjectUsername, request.newRole, ScopeType.ORGANIZATION
    )

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/thread/{threadId}")
    @Operation(summary = "Assign a thread role to user")
    fun changeThreadRole(
        @AuthenticationPrincipal issuer: User,
        @PathVariable threadId: Long,
        @Valid @RequestBody request: ChangeRoleRequest
    ) = rolesService.changeRole(
        issuer, threadId, request.subjectUsername, request.newRole, ScopeType.THREAD
    )

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/deadline/{deadlineId}")
    @Operation(summary = "Assign a deadline role to user")
    fun changeDeadlineRole(
        @AuthenticationPrincipal issuer: User,
        @PathVariable deadlineId: Long,
        @Valid @RequestBody request: ChangeRoleRequest
    ) = rolesService.changeRole(
        issuer, deadlineId, request.subjectUsername, request.newRole, ScopeType.DEADLINE
    )

    @GetMapping
    @Operation(summary = "Get all roles and assignment permissions")
    fun getMetadata(): RolesMetadataResponse = rolesService.metadata
}