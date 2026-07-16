package xyz.om3lette.deadlines_api.data.otp.request

import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.common.validation.KnownPattern
import xyz.om3lette.deadlines_api.data.common.validation.enums.KnownPatternReason
import xyz.om3lette.deadlines_api.data.otp.constraints.OtpConstraints
import java.util.*

data class VerifyOtpRequest(
    val id: UUID,

    @field:KnownPattern(regexp = OtpConstraints.DIGITS_ONLY_PATTERN, reason = KnownPatternReason.DIGITS_ONLY)
    @field:Size(min = OtpConstraints.CODE_LENGTH, max = OtpConstraints.CODE_LENGTH)
    val code: String
)
