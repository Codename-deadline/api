package xyz.om3lette.deadlines_api.data.scopes.deadline.dto

data class DeadlinePermissions(
    val update: Boolean,
    val delete: Boolean,
    val manageAssignees: Boolean,
    val manageAttachments: Boolean
)
