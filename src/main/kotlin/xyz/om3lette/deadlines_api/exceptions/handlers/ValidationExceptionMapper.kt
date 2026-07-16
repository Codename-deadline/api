package xyz.om3lette.deadlines_api.exceptions.handlers

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.core.MethodParameter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.validation.method.ParameterErrors
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import tools.jackson.core.JacksonException
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.module.kotlin.KotlinInvalidNullException
import xyz.om3lette.deadlines_api.data.common.validation.ValidationViolation
import xyz.om3lette.deadlines_api.data.common.validation.ValidationViolationMapper

internal object ValidationExceptionMapper {
    fun from(error: MethodArgumentNotValidException): List<ValidationViolation> =
        fromErrors(error.bindingResult)

    fun from(error: HandlerMethodValidationException): List<ValidationViolation> =
        error.parameterValidationResults.flatMap { result ->
            if (result is ParameterErrors) {
                fromErrors(result)
            } else {
                val field = parameterName(result.methodParameter)
                result.resolvableErrors.map { resolvable ->
                    try {
                        ValidationViolationMapper.fromConstraint(
                            field,
                            result.unwrap(resolvable, ConstraintViolation::class.java)
                        )
                    } catch (_: IllegalArgumentException) {
                        ValidationViolationMapper.invalid(field)
                    }
                }
            }
        }

    fun from(error: ConstraintViolationException): List<ValidationViolation> =
        error.constraintViolations.map {
            ValidationViolationMapper.fromConstraint(clientConstraintPath(it.propertyPath.toString()), it)
        }

    fun from(error: MethodArgumentTypeMismatchException): ValidationViolation =
        typeOrFormat(error.name, error.requiredType ?: Any::class.java)

    fun from(error: HttpMessageNotReadableException): ValidationViolation? {
        val jacksonError = generateSequence(error.cause) { it.cause }
            .filterIsInstance<JacksonException>()
            .firstOrNull()
            ?: return null
        val field = jacksonPath(jacksonError) ?: return null

        return when (jacksonError) {
            is KotlinInvalidNullException -> ValidationViolationMapper.required(field)
            is InvalidFormatException -> if (jacksonError.targetType.isEnum) {
                ValidationViolationMapper.oneOf(field, jacksonError.targetType)
            } else {
                typeOrFormat(field, jacksonError.targetType)
            }
            is MismatchedInputException -> typeOrFormat(field, jacksonError.targetType)
            else -> null
        }
    }

    private fun fromErrors(errors: Errors): List<ValidationViolation> =
        errors.allErrors.map { error ->
            fromObjectError(error, (error as? FieldError)?.let { errors.getFieldType(it.field) })
        }

    private fun fromObjectError(error: ObjectError, expectedType: Class<*>?): ValidationViolation {
        val field = (error as? FieldError)?.field ?: error.objectName
        if (error.contains(ConstraintViolation::class.java)) {
            return ValidationViolationMapper.fromConstraint(
                field,
                error.unwrap(ConstraintViolation::class.java)
            )
        }

        return if ((error as? FieldError)?.isBindingFailure == true) {
            ValidationViolationMapper.type(field, expectedType ?: Any::class.java)
        } else {
            ValidationViolationMapper.invalid(field)
        }
    }

    private fun typeOrFormat(field: String, type: Class<*>): ValidationViolation =
        ValidationViolationMapper.format(field, type) ?: ValidationViolationMapper.type(field, type)

    private fun parameterName(parameter: MethodParameter): String {
        val requestParam = parameter.getParameterAnnotation(RequestParam::class.java)
        val pathVariable = parameter.getParameterAnnotation(PathVariable::class.java)
        return requestParam?.name?.takeIf(String::isNotBlank)
            ?: requestParam?.value?.takeIf(String::isNotBlank)
            ?: pathVariable?.name?.takeIf(String::isNotBlank)
            ?: pathVariable?.value?.takeIf(String::isNotBlank)
            ?: parameter.parameterName
            ?: parameter.parameter.type.simpleName.replaceFirstChar(Char::lowercase)
    }

    private fun clientConstraintPath(path: String): String = path.substringAfter('.', path)

    private fun jacksonPath(error: JacksonException): String? {
        val path = error.path
        if (path.isEmpty()) return (error as? KotlinInvalidNullException)?.kotlinPropertyName

        return buildString {
            path.forEach { reference ->
                reference.propertyName?.let {
                    if (isNotEmpty()) append('.')
                    append(it)
                }
                if (reference.index >= 0) append('[').append(reference.index).append(']')
            }
        }.takeIf(String::isNotEmpty)
    }
}
