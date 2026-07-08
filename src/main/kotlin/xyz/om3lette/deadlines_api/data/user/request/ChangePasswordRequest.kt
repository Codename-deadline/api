package xyz.om3lette.deadlines_api.data.user.request

import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.user.constraints.PasswordConstraints

data class ChangePasswordRequest(
    @field:Size(min = PasswordConstraints.PASSWORD_MIN, max = PasswordConstraints.PASSWORD_MAX)
    val oldPassword: String?,

    @field:Size(min = PasswordConstraints.PASSWORD_MIN, max = PasswordConstraints.PASSWORD_MAX)
    val newPassword: String
)
