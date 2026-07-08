package xyz.om3lette.deadlines_api.data.scopes.deadline.requests

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeTextConstraints
import java.time.Instant

data class PatchDeadlineRequest(
    @field:Pattern(regexp = ScopeTextConstraints.TITLE_PATCH_REGEX)
    @field:Size(min = ScopeTextConstraints.TITLE_MIN, max = ScopeTextConstraints.TITLE_MAX)
    val title: String?,

    @field:Size(max = ScopeTextConstraints.DESCRIPTION_MAX)
    val description: String?,

    val isCompleted: Boolean?,
    val due: Instant?,
)
