package xyz.om3lette.deadlines_api.data.otp.request

import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel

data class CreateOtpRequest(
    val identifier: String,

    val channel: OtpChannel,

    val username: String
)
