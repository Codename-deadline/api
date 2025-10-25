package xyz.om3lette.deadlines_api.data.scopes.thread.response

data class ThreadResponse(
    val id: Long,

    val title: String,

    val description: String?,

    val organizationId: Long
)
