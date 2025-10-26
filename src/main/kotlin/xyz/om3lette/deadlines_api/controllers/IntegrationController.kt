package xyz.om3lette.deadlines_api.controllers

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.integration.request.LinkMessengerAccountRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.integration.IntegrationService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/integration")
@Tag(name = "Integrations", description = "Endpoints for linking/managing other platforms integration")
class IntegrationController(
    private val integrationService: IntegrationService
) {
    @PostMapping("/account")
    fun linkAccount(
        @AuthenticationPrincipal user: User,
        @RequestBody request: LinkMessengerAccountRequest
    ) = integrationService.handleLinkMessengerAccountRequest(user, request.accountId, request.messenger)
}