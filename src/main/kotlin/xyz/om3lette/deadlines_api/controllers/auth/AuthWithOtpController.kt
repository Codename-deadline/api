package xyz.om3lette.deadlines_api.controllers.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.otp.request.CompletePasswordRequest
import xyz.om3lette.deadlines_api.data.otp.request.RegisterOtpRequest
import xyz.om3lette.deadlines_api.services.auth.otp.OtpService

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
class AuthWithOtpController(
    private val otpService: OtpService
) {
    @PostMapping("/register-otp")
    @Operation(
        summary = "Initiates user registration",
        description = "Creates a registration request and send confirmation request with otp."
    )
    fun registerOtp(
        @RequestBody request: RegisterOtpRequest
    ) = otpService.createRegisterRequest(
        request.identifier,
        request.channel,
        request.username,
        request.fullName,
        request.language
    )

    @PostMapping("/verify-password")
    @Operation(
        summary = "Verifies the password",
        description = "If user has a password set sign in requires the password verification on top of otp."
    )
    fun completePassword(
        @RequestBody request: CompletePasswordRequest
    ) = otpService.completePassword(request.id, request.password)
}