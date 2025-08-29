package xyz.om3lette.deadlines_api.controllers.auth

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.otp.request.CompletePasswordRequest
import xyz.om3lette.deadlines_api.data.otp.request.RegisterOtpRequest
import xyz.om3lette.deadlines_api.services.auth.otp.OtpService

@RestController
@RequestMapping("/api/auth")
class AuthWithOtpController(
    private val otpService: OtpService
) {
    @PostMapping("/register-otp")
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
    fun completePassword(
        @RequestBody request: CompletePasswordRequest
    ) = otpService.completePassword(request.id, request.password)
}