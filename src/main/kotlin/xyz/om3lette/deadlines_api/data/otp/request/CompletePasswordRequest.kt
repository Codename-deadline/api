package xyz.om3lette.deadlines_api.data.otp.request

import java.util.UUID

data class CompletePasswordRequest(
    val id: UUID,

    val password: String
)
