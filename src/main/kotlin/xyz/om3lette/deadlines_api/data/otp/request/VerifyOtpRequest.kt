package xyz.om3lette.deadlines_api.data.otp.request

import jakarta.validation.constraints.Pattern
import java.util.UUID

data class VerifyOtpRequest(
    val id: UUID,

    @field:Pattern(regexp = "\\d{6}")
    val code: String
)
