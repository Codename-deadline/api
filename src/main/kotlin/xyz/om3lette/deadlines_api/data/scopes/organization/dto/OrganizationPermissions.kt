package xyz.om3lette.deadlines_api.data.scopes.organization.dto

data class OrganizationPermissions(
    val update: Boolean,
    val delete: Boolean,
    val manageRoles: Boolean,
    val invite: Boolean
)
