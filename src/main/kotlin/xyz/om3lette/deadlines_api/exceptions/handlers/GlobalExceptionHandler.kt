package xyz.om3lette.deadlines_api.exceptions.handlers

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException

@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(StatusCodeException::class)
    fun handleStatusCodeException(error: StatusCodeException): ResponseEntity<Any> {
        return ResponseEntity.status(error.statusCode).body(error.getResponse())
    }

    @ExceptionHandler(NumberFormatException::class)
    fun handleStatusCodeException(error: NumberFormatException): ResponseEntity<Any> {
        return ResponseEntity.status(400).body(mapOf("type" to "error", "data" to "Invalid number format"))
    }
}