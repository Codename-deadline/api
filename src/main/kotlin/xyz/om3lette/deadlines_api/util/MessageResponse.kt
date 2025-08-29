package xyz.om3lette.deadlines_api.util

data class MessageResponse(
    val type: String,
    val data: Any
) {
    companion object {
        fun error(message: String) = MessageResponse("error", message)

        fun success(message: Any = "OK") = MessageResponse("success", message)
    }
}
