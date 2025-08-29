package xyz.om3lette.deadlines_api.data.user.request

import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language


data class RegisterRequest(
    @field:Size(min = 3, max = 32)
    val username: String,

    @field:Size(min = 1, max = 64)
    val fullName: String,

    @field:Size(min = 8, max = 64)
    val password: String,

    val language: Language?
)
