package xyz.om3lette.deadlines_api.data.user.response

import java.time.Instant

data class UserResponse(
    val id: Long,

    val username: String,

    val fullName: String,

    val joinedAt: Instant
)
