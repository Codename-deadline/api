package xyz.om3lette.deadlines_api.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.constraints.UserConstraints
import xyz.om3lette.deadlines_api.data.user.request.PatchUserRequest
import xyz.om3lette.deadlines_api.services.UserService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/user")
@Tag(name = "User")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    @Operation(summary = "Get public user data")
    fun getUser(
        @AuthenticationPrincipal user: User
    ) = user.toResponse()

    @DeleteMapping
    @Operation(summary = "Delete user")
    fun deleteUser(
        @AuthenticationPrincipal user: User
    ) = userService.deleteUser(user)

    @PatchMapping
    @Operation(summary = "Update user info")
    fun patchUser(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: PatchUserRequest
    ) = userService.patchUser(user, request.username, request.fullName, request.language, request.timeZone)

    @GetMapping("/linked-accounts")
    @Operation(summary = "Get user's linked messenger accounts")
    fun getMessengerAccounts(
        @AuthenticationPrincipal user: User
    ) = userService.getMessengerAccounts(user)

    @GetMapping("/hints")
    @Operation(summary = "Get a batch of usernames starting with")
    fun getUsernamesStartingWith(
        @RequestParam
        @Size(min = UserConstraints.USERNAME_HINT_MIN, max = UserConstraints.USERNAME_HINT_MAX)
        startsWith: String
    ) = userService.getUsernamesStartingWith(startsWith)
}
