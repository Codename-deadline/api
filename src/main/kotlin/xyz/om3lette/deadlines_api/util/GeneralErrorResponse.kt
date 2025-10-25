package xyz.om3lette.deadlines_api.util

import org.springframework.web.ErrorResponse
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException

data class GeneralErrorResponse(
    val detail: String,

    val type: String = "error"
) {
    companion object {
        fun fromStatusCodeException(error: StatusCodeException): GeneralErrorResponse =
            GeneralErrorResponse(error.detail)

        fun fromErrorResponse(error: ErrorResponse): GeneralErrorResponse =
            GeneralErrorResponse(error.body.detail ?: "No details available.")
    }
}
