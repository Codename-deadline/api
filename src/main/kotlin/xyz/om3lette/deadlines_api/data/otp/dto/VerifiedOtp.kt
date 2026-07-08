package xyz.om3lette.deadlines_api.data.otp.dto

import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpPurpose

data class VerifiedOtp(
    val purpose: OtpPurpose,
    val context: String
)
