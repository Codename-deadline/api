package xyz.om3lette.deadlines_api.data.user.response

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import java.time.Instant

data class UserResponse(
    val id: Long,

    val username: String,

    val fullName: String,

    val joinedAt: Instant,

    val language: Language
)
