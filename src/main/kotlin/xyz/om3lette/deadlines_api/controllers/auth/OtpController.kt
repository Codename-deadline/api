package xyz.om3lette.deadlines_api.controllers.auth

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.otp.request.CreateOtpRequest
import xyz.om3lette.deadlines_api.data.otp.request.VerifyOtpRequest
import xyz.om3lette.deadlines_api.services.auth.otp.OtpService

@RestController
@RequestMapping("/api/auth/otp")
@Tag(name = "Authentication")
class OtpController(
    private val otpService: OtpService
) {
    @PostMapping
    fun signInOtp(
        @RequestBody request: CreateOtpRequest
    ) = otpService.createAndSendOtpForUser(
        request.identifier,
        request.channel,
        request.username
    )

    @PostMapping("/verify")
    fun verify(
        @RequestBody request: VerifyOtpRequest
    ) = otpService.signInOtp(request.id, request.code)
}