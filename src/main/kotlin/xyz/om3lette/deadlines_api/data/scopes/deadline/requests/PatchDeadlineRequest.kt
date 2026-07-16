package xyz.om3lette.deadlines_api.data.scopes.deadline.requests

import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.common.validation.KnownPattern
import xyz.om3lette.deadlines_api.data.common.validation.enums.KnownPatternReason
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeTextConstraints
import java.time.Instant

data class PatchDeadlineRequest(
    @field:KnownPattern(
        regexp = ScopeTextConstraints.TITLE_PATCH_REGEX,
        reason = KnownPatternReason.NOT_BLANK
    )
    @field:Size(min = ScopeTextConstraints.TITLE_MIN, max = ScopeTextConstraints.TITLE_MAX)
    val title: String?,

    @field:Size(max = ScopeTextConstraints.DESCRIPTION_MAX)
    val description: String?,

    val isCompleted: Boolean?,
    val due: Instant?,
)
