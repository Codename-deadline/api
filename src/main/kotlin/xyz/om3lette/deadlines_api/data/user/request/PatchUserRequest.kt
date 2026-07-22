package xyz.om3lette.deadlines_api.data.user.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.user.constraints.UserConstraints

data class PatchUserRequest(
    @field:Size(min = UserConstraints.USERNAME_MIN, max = UserConstraints.USERNAME_MAX)
    val username: String?,

    @field:Size(min = UserConstraints.FULL_NAME_MIN, max = UserConstraints.FULL_NAME_MAX)
    val fullName: String?,

    val language: Language?,
)
