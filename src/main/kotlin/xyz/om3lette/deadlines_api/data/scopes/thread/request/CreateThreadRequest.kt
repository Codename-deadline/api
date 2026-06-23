package xyz.om3lette.deadlines_api.data.scopes.thread.request

import xyz.om3lette.deadlines_api.data.scopes.common.dto.UsernameRolePairList

data class CreateThreadRequest(
    val title: String,
    val description: String?,
    val invitations: UsernameRolePairList
)
