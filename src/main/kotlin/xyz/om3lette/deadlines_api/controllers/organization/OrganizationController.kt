package xyz.om3lette.deadlines_api.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.common.constraints.PaginationConstraints
import xyz.om3lette.deadlines_api.data.scopes.organization.request.CreateOrganizationRequest
import xyz.om3lette.deadlines_api.data.scopes.organization.request.PatchOrganizationRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.OrganizationService
import kotlin.math.min

@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/organizations")
@Tag(name = "Organizations")
class OrganizationController(
    private val organizationService: OrganizationService
) {
    @GetMapping
    @Operation(summary = "Get organizations where user is a member")
    fun getOrganizationsByUser(
        @AuthenticationPrincipal user: User,
        @RequestParam("page") @Min(PaginationConstraints.PAGE_MIN) pageNumber: Int
    ) = organizationService.getOrganizationsByUser(user, pageNumber, 10)

    @GetMapping("/{organizationId}")
    @Operation(summary = "Get organization metadata")
    fun getOrganizationMetadata(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive organizationId: Long
    ) = organizationService.getOrganization(user, organizationId)

    @PostMapping
    @Operation(summary = "Create a new organization")
    fun createOrganization(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: CreateOrganizationRequest
    ) = organizationService.createOrganization(
        user,
        request.title,
        request.description,
        request.type,
        request.invitations
    )

    @DeleteMapping("/{organizationId}")
    @Operation(summary = "Delete an organization")
    fun deleteOrganization(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive organizationId: Long
    ) = organizationService.deleteOrganization(user, organizationId)

    @PatchMapping("/{organizationId}")
    @Operation(summary = "Update organization metadata")
    fun patchOrganization(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive organizationId: Long,
        @Valid @RequestBody request: PatchOrganizationRequest
    ) = organizationService.patchOrganization(user, organizationId, request.title, request.description)

    @DeleteMapping("/{organizationId}/members/{memberUsername}")
    @Operation(summary = "Remove member")
    fun removeMember(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive organizationId: Long,
        @PathVariable memberUsername: String
    ) = organizationService.removeMember(user, organizationId, memberUsername)

    @GetMapping("/{organizationId}/members")
    @Operation(
        summary = "Get all organization members",
        description = "Returns a list of organization members. Higher role users are not included."
    )
    fun getMembers(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive organizationId: Long,
        @RequestParam("page") @Min(PaginationConstraints.PAGE_MIN) pageNumber: Int,
        @RequestParam("size")
        @Min(PaginationConstraints.PAGE_SIZE_MIN)
        @Max(PaginationConstraints.PAGE_SIZE_MAX)
        pageSize: Int
    ) = organizationService.getOrganizationMembers(user, organizationId, pageNumber, pageSize)

}
