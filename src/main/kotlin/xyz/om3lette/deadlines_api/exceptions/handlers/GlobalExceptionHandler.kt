package xyz.om3lette.deadlines_api.exceptions.handlers

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.util.GeneralErrorResponse

@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(StatusCodeException::class)
    fun handleStatusCodeException(error: StatusCodeException): ResponseEntity<GeneralErrorResponse> {
        return ResponseEntity.status(error.statusCode).body(
            GeneralErrorResponse.fromStatusCodeException(error)
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(exception: Exception): ResponseEntity<Any> =
        when {
            exception is ErrorResponse -> ResponseEntity.status(exception.statusCode).body(
                GeneralErrorResponse.fromErrorResponse(exception)
            )
            else -> ResponseEntity.status(500).body(
                GeneralErrorResponse("No details available.")
            )
        }
}