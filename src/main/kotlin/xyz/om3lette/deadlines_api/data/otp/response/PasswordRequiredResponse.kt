package xyz.om3lette.deadlines_api.data.otp.response

import java.util.UUID

data class PasswordRequiredResponse(
    val requestId: UUID,

    val passwordRequired: Boolean = true
)
