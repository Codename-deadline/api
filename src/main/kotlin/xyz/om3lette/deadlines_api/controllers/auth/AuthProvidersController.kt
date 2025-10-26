package xyz.om3lette.deadlines_api.controllers.auth

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.otp.request.provider.TmaRegisterRequest
import xyz.om3lette.deadlines_api.services.auth.providers.tma.TmaAuthProvider

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
class AuthProvidersController(
    private val tmaAuthProvider: TmaAuthProvider
) {
    @PostMapping("/register-tma")
    fun tmaRegister(
        @RequestBody request: TmaRegisterRequest
    ) = tmaAuthProvider.register(request.initData, request.username)
}