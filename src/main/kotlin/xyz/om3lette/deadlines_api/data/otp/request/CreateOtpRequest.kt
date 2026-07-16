package xyz.om3lette.deadlines_api.data.otp.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.common.validation.KnownPattern
import xyz.om3lette.deadlines_api.data.common.validation.enums.KnownPatternReason
import xyz.om3lette.deadlines_api.data.otp.constraints.OtpConstraints
import xyz.om3lette.deadlines_api.data.user.constraints.UserConstraints
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel

data class CreateOtpRequest(
    @field:NotBlank
    @field:KnownPattern(
        regexp = OtpConstraints.DIGITS_ONLY_PATTERN,
        reason = KnownPatternReason.DIGITS_ONLY
    )
    @field:Size(max = OtpConstraints.IDENTIFIER_MAX)
    val identifier: String,

    val channel: OtpChannel,

    @field:NotBlank
    @field:Size(min = UserConstraints.USERNAME_MIN, max = UserConstraints.USERNAME_MAX)
    val username: String
)
