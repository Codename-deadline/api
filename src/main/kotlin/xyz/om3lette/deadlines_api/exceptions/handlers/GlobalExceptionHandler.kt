package xyz.om3lette.deadlines_api.exceptions.handlers

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import xyz.om3lette.deadlines_api.data.common.validation.ValidationErrorResponse
import xyz.om3lette.deadlines_api.data.common.validation.ValidationViolation
import xyz.om3lette.deadlines_api.data.common.validation.ValidationViolationMapper
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.util.GeneralErrorResponse

@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(StatusCodeException::class)
    fun handleStatusCodeException(error: StatusCodeException): ResponseEntity<GeneralErrorResponse> =
        ResponseEntity.status(error.statusCode).body(
            GeneralErrorResponse.fromStatusCodeException(error)
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(error: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> =
        validationResponse(ValidationExceptionMapper.from(error))

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(error: HandlerMethodValidationException): ResponseEntity<ValidationErrorResponse> =
        validationResponse(ValidationExceptionMapper.from(error))

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(error: ConstraintViolationException): ResponseEntity<ValidationErrorResponse> =
        validationResponse(ValidationExceptionMapper.from(error))

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(
        error: MissingServletRequestParameterException
    ): ResponseEntity<ValidationErrorResponse> = validationResponse(
        listOf(ValidationViolationMapper.required(error.parameterName))
    )

    @ExceptionHandler(MissingServletRequestPartException::class)
    fun handleMissingServletRequestPart(error: MissingServletRequestPartException): ResponseEntity<ValidationErrorResponse> =
        validationResponse(listOf(ValidationViolationMapper.required(error.requestPartName)))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(error: MethodArgumentTypeMismatchException): ResponseEntity<ValidationErrorResponse> =
        validationResponse(listOf(ValidationExceptionMapper.from(error)))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(error: HttpMessageNotReadableException): ResponseEntity<*> {
        val violation = ValidationExceptionMapper.from(error) ?: return deserializationResponse()
        return validationResponse(listOf(violation))
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(exception: Exception): ResponseEntity<GeneralErrorResponse> =
        when (exception) {
            is ErrorResponse -> ResponseEntity.status(exception.statusCode).body(
                GeneralErrorResponse.fromErrorResponse(exception)
            )

            else -> {
                logger.error("Unhandled exception while processing request", exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    GeneralErrorResponse(code = ErrorCode.UNKNOWN_ERROR, detail = "No details available.")
                )
            }
        }

    private fun validationResponse(violations: List<ValidationViolation>): ResponseEntity<ValidationErrorResponse> =
        ResponseEntity.unprocessableContent().body(
            ValidationErrorResponse(violations = ValidationViolationMapper.sorted(violations))
        )

    private fun deserializationResponse(): ResponseEntity<GeneralErrorResponse> =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            GeneralErrorResponse(code = ErrorCode.DESERIALIZATION_ERROR)
        )
}
