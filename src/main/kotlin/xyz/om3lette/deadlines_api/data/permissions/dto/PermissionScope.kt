package xyz.om3lette.deadlines_api.data.permissions.dto

import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread

sealed interface PermissionScope

data class OrganizationScope(val orgId: Long) : PermissionScope

data class ThreadScope(val thread: Thread) : PermissionScope

data class DeadlineScope(val deadline: Deadline) : PermissionScope
