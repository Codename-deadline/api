package xyz.om3lette.deadlines_api.exceptions.type

import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode

data class StatusCodeException(
    val statusCode: Int,
    val code: ErrorCode,
    val detail: String? = null,
    val params: Map<String, Any> = emptyMap()
) : RuntimeException(
    "Status code: $statusCode, detail: $detail, code: $code"
)
