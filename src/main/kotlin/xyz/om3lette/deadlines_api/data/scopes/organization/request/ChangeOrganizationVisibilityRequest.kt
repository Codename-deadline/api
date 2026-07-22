package xyz.om3lette.deadlines_api.data.scopes.organization.request

import jakarta.validation.constraints.NotNull
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType

data class ChangeOrganizationVisibilityRequest(
    @field:NotNull
    val type: OrganizationType
)
