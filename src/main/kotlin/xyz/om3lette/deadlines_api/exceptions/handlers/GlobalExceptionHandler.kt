package xyz.om3lette.deadlines_api.exceptions.handlers

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.util.GeneralErrorResponse

@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

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
            exception is HttpMessageNotReadableException -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                GeneralErrorResponse(code = ErrorCode.DESERIALIZATION_ERROR)
            )
            else -> {
                logger.error("Unhandled exception while processing request", exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    GeneralErrorResponse(code = ErrorCode.UNKNOWN_ERROR, detail = "No details available.")
                )
            }
        }
}