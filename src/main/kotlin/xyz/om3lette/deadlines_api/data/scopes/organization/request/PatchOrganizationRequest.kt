package xyz.om3lette.deadlines_api.data.scopes.organization.request

data class PatchOrganizationRequest(
    val title: String?,
    val description: String?
)