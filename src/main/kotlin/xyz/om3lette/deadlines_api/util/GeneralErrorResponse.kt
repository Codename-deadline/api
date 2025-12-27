package xyz.om3lette.deadlines_api.util

import org.springframework.web.ErrorResponse
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException

data class GeneralErrorResponse(
    val code: ErrorCode,

    val detail: String? = null,

    val params: Map<String, Any> = emptyMap(),

    val type: String = "error",
) {
    companion object {
        fun fromStatusCodeException(error: StatusCodeException): GeneralErrorResponse =
            GeneralErrorResponse(code = error.code, detail = error.detail, params = error.params)

        fun fromErrorResponse(error: ErrorResponse): GeneralErrorResponse =
            GeneralErrorResponse(code = ErrorCode.UNKNOWN_ERROR, detail = error.body.detail ?: "No details available.")
    }
}
