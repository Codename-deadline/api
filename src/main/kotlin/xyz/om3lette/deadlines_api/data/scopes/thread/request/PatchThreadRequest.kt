package xyz.om3lette.deadlines_api.data.scopes.thread.request

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeTextConstraints

data class PatchThreadRequest(
    @field:Pattern(regexp = ScopeTextConstraints.TITLE_PATCH_REGEX)
    @field:Size(min = ScopeTextConstraints.TITLE_MIN, max = ScopeTextConstraints.TITLE_MAX)
    val title: String?,

    @field:Size(max = ScopeTextConstraints.DESCRIPTION_MAX)
    val description: String?
)
