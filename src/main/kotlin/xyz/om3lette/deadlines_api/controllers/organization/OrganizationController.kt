package xyz.om3lette.deadlines_api.controllers.organization

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.scopes.organization.request.CreateOrganizationRequest
import xyz.om3lette.deadlines_api.data.scopes.organization.request.PatchOrganizationRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.OrganizationService

@RestController
@RequestMapping("/api/organization")
class OrganizationController(
    private val organizationService: OrganizationService
) {
    @GetMapping("/{organizationId}")
    fun getOrganizationMetadata(
        @AuthenticationPrincipal user: User,
        @PathVariable organizationId: Long
    ) = organizationService.getOrganizationMetaData(user, organizationId)

    @PostMapping
    fun createOrganization(
        @AuthenticationPrincipal user: User,
        @RequestBody request: CreateOrganizationRequest
    ) = organizationService.createOrganization(
        user,
        request.title,
        request.description,
        request.type,
        request.usernamesToInvite
    )

    @DeleteMapping("/{organizationId}")
    fun deleteOrganization(
        @AuthenticationPrincipal user: User,
        @PathVariable organizationId: Long
    ) = organizationService.deleteOrganization(user, organizationId)

    @PatchMapping("/{organizationId}")
    fun patchOrganization(
        @AuthenticationPrincipal user: User,
        @PathVariable organizationId: Long,
        @RequestBody request: PatchOrganizationRequest
    ) = organizationService.patchOrganization(user, organizationId, request.title, request.description)

    @DeleteMapping("/{organizationId}/members/{memberUsername}")
    fun removeMember(
        @AuthenticationPrincipal user: User,
        @PathVariable organizationId: Long,
        @PathVariable memberUsername: String
    ) = organizationService.removeMember(user, organizationId, memberUsername)

    @GetMapping("/{organizationId}/members")
    fun getMembers(
        @AuthenticationPrincipal user: User,
        @PathVariable organizationId: Long,
        @RequestParam("page") pageNumber: Int
    ) = organizationService.getOrganizationMembers(user, organizationId, pageNumber, 10)

}