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
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.util.GeneralErrorResponse

@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun validationField(field: String, code: String, message: String?): Map<String, Any> = buildMap {
        put("field", field)
        put("code", code.lowercase())
        if (message != null) put("message", message)
    }

    private fun validationResponse(fields: List<Map<String, Any>>): ResponseEntity<GeneralErrorResponse> =
        ResponseEntity.unprocessableContent().body(
            GeneralErrorResponse(
                code = ErrorCode.VALIDATION_FAILED,
                params = mapOf("fields" to fields)
            )
        )

    @ExceptionHandler(StatusCodeException::class)
    fun handleStatusCodeException(error: StatusCodeException): ResponseEntity<GeneralErrorResponse> {
        return ResponseEntity.status(error.statusCode).body(
            GeneralErrorResponse.fromStatusCodeException(error)
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(error: MethodArgumentNotValidException): ResponseEntity<GeneralErrorResponse> {
        val fieldErrors = error.bindingResult.fieldErrors.map {
            validationField(it.field, it.code ?: "invalid", it.defaultMessage)
        }
        val globalErrors = error.bindingResult.globalErrors.map {
            validationField(it.objectName, it.code ?: "invalid", it.defaultMessage)
        }
        return validationResponse(fieldErrors + globalErrors)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(error: HandlerMethodValidationException): ResponseEntity<GeneralErrorResponse> {
        val fields = error.parameterValidationResults.flatMap { result ->
            val field = result.methodParameter.parameterName ?: result.methodParameter.parameter.type.simpleName
            result.resolvableErrors.map {
                validationField(field, it.codes?.firstOrNull()?.substringAfterLast('.') ?: "invalid", it.defaultMessage)
            }
        }
        return validationResponse(fields)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(error: ConstraintViolationException): ResponseEntity<GeneralErrorResponse> =
        validationResponse(
            error.constraintViolations.map {
                validationField(
                    field = it.propertyPath.toString().substringAfterLast('.'),
                    code = it.constraintDescriptor.annotation.annotationClass.simpleName ?: "invalid",
                    message = it.message
                )
            }
        )

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(
        error: MissingServletRequestParameterException
    ): ResponseEntity<GeneralErrorResponse> = validationResponse(
        listOf(validationField(error.parameterName, "missing", error.message))
    )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(error: MethodArgumentTypeMismatchException): ResponseEntity<GeneralErrorResponse> =
        validationResponse(
            listOf(validationField(error.name, "type-mismatch", error.message))
        )

    @ExceptionHandler(Exception::class)
    fun handleAny(exception: Exception): ResponseEntity<Any> =
        when (exception) {
            is ErrorResponse -> ResponseEntity.status(exception.statusCode).body(
                GeneralErrorResponse.fromErrorResponse(exception)
            )

            is HttpMessageNotReadableException -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
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
