package xyz.om3lette.deadlines_api.data.scopes.thread.dto

data class ThreadPermissions(
    val update: Boolean,
    val delete: Boolean,
    val manageAssignees: Boolean,
    val createDeadlines: Boolean,
)
