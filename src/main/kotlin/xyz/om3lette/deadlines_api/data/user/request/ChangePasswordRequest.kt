package xyz.om3lette.deadlines_api.data.user.request

import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    val oldPassword: String?,

    @field:Size(min = 8, max = 64)
    val newPassword: String
)
