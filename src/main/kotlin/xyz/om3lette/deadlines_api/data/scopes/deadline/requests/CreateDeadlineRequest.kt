package xyz.om3lette.deadlines_api.data.scopes.deadline.requests

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.scopes.common.dto.UsernameRolePairList
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeTextConstraints
import java.time.Instant

data class CreateDeadlineRequest(
    @field:NotBlank
    @field:Size(min = ScopeTextConstraints.TITLE_MIN, max = ScopeTextConstraints.TITLE_MAX)
    val title: String,

    @field:Size(max = ScopeTextConstraints.DESCRIPTION_MAX)
    val description: String?,

    val due: Instant,

    @field:Valid
    val invitations: UsernameRolePairList
)
