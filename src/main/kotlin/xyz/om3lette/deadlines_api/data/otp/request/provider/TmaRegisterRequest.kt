package xyz.om3lette.deadlines_api.data.otp.request.provider

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.otp.constraints.OtpConstraints
import xyz.om3lette.deadlines_api.data.user.constraints.UserConstraints

data class TmaRegisterRequest(
    @field:NotBlank
    @field:Size(max = OtpConstraints.TMA_INIT_DATA_MAX)
    val initData: String,

    @field:Size(min = UserConstraints.USERNAME_MIN, max = UserConstraints.USERNAME_MAX)
    val username: String?
)
