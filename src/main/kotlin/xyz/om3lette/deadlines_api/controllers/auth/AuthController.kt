package xyz.om3lette.deadlines_api.controllers.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.request.ChangePasswordRequest
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.auth.AccountSecurityService
import xyz.om3lette.deadlines_api.services.auth.AuthSessionService

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
class AuthController(
    private val authSessionService: AuthSessionService,
    private val accountSecurityService: AccountSecurityService
) {
    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token")
    fun refreshToken(
        request: HttpServletRequest
    ) = authSessionService.refreshSession(extractBearerToken(request))

    @GetMapping("/sign-out")
    @Operation(summary = "Invalidate all user's refresh tokens")
    fun signOut(
        @AuthenticationPrincipal user: User
    ) = authSessionService.revokeAllSessions(user)

    @PostMapping("/change-password")
    @Operation(
        summary = "Change password",
        description = "If user does not have a password set omit oldPassword from the request body",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun changePassword(
        @RequestBody @Valid request: ChangePasswordRequest,
        @AuthenticationPrincipal user: User
    ) = accountSecurityService.changePassword(user, request.oldPassword, request.newPassword)

    private fun extractBearerToken(request: HttpServletRequest): String {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw StatusCodeException(401, ErrorCode.AUTH_INVALID_CREDENTIALS)
        }
        return authHeader.substring(7)
    }
}
