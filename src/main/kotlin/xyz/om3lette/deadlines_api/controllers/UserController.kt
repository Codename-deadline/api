package xyz.om3lette.deadlines_api.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.UserService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/user")
@Tag(name = "User")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    @Operation(summary = "Get public user data")
    fun getUser(
        @AuthenticationPrincipal user: User
    ) = user.toResponse()
}
