package xyz.om3lette.deadlines_api.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.dto.UnlinkMessengerAccountRequest
import xyz.om3lette.deadlines_api.data.integration.request.LinkMessengerAccountRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.integration.MessengerAccountLinkingService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/integration")
@Tag(name = "Integrations", description = "Endpoints for linking/managing other platforms integration")
class IntegrationController(
    private val messengerAccountLinkingService: MessengerAccountLinkingService
) {
    @PostMapping("/accounts")
    @Operation(summary = "Link the external platform account to user")
    fun linkAccount(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: LinkMessengerAccountRequest
    ) = messengerAccountLinkingService.sendConfirmationForAccountLinkage(user, request.accountId, request.messenger)

    @DeleteMapping("/accounts")
    @Operation(summary = "Unlink an external platform account from user")
    fun unlinkAccount(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: UnlinkMessengerAccountRequest
    ) = messengerAccountLinkingService.unlinkMessengerAccount(user, request.accountId, request.messenger)
}
