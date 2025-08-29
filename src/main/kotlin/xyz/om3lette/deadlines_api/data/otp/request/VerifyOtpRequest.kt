package xyz.om3lette.deadlines_api.data.otp.request

import java.util.UUID

data class VerifyOtpRequest(
    val id: UUID,

    val code: String
)
