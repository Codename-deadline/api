package xyz.om3lette.deadlines_api.data.scopes.thread.request

data class CreateThreadRequest(
    val title: String,
    val description: String?,
    val usernamesToAssign: List<String>
)
