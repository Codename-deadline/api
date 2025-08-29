package xyz.om3lette.deadlines_api.exceptions.type

import xyz.om3lette.deadlines_api.util.MessageResponse

data class StatusCodeException(
    val statusCode: Int,
    val detail: String
) : RuntimeException(
    "Status code: $statusCode, detail: $detail"
) {
    fun getResponse(): MessageResponse = MessageResponse.error(detail)
}
