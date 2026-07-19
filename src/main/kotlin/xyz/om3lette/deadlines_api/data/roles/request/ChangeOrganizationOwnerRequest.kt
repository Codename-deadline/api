package xyz.om3lette.deadlines_api.data.roles.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.user.constraints.UserConstraints

data class ChangeOrganizationOwnerRequest(
    @field:NotBlank
    @field:Size(min = UserConstraints.USERNAME_MIN, max = UserConstraints.USERNAME_MAX)
    val newOwnerUsername: String
)
