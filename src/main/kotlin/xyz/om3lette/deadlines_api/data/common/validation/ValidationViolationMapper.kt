package xyz.om3lette.deadlines_api.data.common.validation

import jakarta.validation.ConstraintViolation
import jakarta.validation.constraints.*
import xyz.om3lette.deadlines_api.data.common.validation.enums.KnownPatternReason
import java.time.temporal.TemporalAccessor
import java.util.*


object ValidationViolationMapper {
    fun fromConstraint(field: String, violation: ConstraintViolation<*>): ValidationViolation {
        return when (val annotation = violation.constraintDescriptor.annotation) {
            is NotNull, is NotEmpty -> simple(field, SimpleValidationReason.REQUIRED)
            is NotBlank -> simple(field, SimpleValidationReason.NOT_BLANK)
            is Positive -> simple(field, SimpleValidationReason.VALUE_POSITIVE)
            is Min -> MinimumValidationViolation(field, MinimumValidationReason.VALUE_MIN, annotation.value)
            is Max -> MaximumValidationViolation(field, MaximumValidationReason.VALUE_MAX, annotation.value)
            is Size -> size(field, violation.invalidValue, annotation.min, annotation.max)
            is KnownPattern -> simple(
                field,
                when (annotation.reason) {
                    KnownPatternReason.DIGITS_ONLY -> SimpleValidationReason.FORMAT_DIGITS_ONLY
                    KnownPatternReason.NOT_BLANK -> SimpleValidationReason.NOT_BLANK
                }
            )
            else -> simple(field, SimpleValidationReason.INVALID)
        }
    }

    private fun simple(field: String, reason: SimpleValidationReason) = SimpleValidationViolation(field, reason)

    fun required(field: String): ValidationViolation = simple(field, SimpleValidationReason.REQUIRED)

    fun invalid(field: String): ValidationViolation = simple(field, SimpleValidationReason.INVALID)

    fun type(field: String, type: Class<*>): ValidationViolation = TypeValidationViolation(
        field = field,
        expected = expectedType(type)
    )

    fun oneOf(field: String, enumType: Class<*>): ValidationViolation = OneOfValidationViolation(
        field = field,
        allowed = enumType.enumConstants.map { (it as Enum<*>).name }
    )

    fun format(field: String, type: Class<*>): ValidationViolation? = when {
        UUID::class.java.isAssignableFrom(type) -> simple(field, SimpleValidationReason.FORMAT_UUID)
        TemporalAccessor::class.java.isAssignableFrom(type) -> simple(field, SimpleValidationReason.FORMAT_TIMESTAMP)
        else -> null
    }

    private fun Class<*>.boxed(): Class<*> =
        when (this) {
            Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
            Byte::class.javaPrimitiveType -> Byte::class.javaObjectType
            Short::class.javaPrimitiveType -> Short::class.javaObjectType
            Int::class.javaPrimitiveType -> Int::class.javaObjectType
            Long::class.javaPrimitiveType -> Long::class.javaObjectType
            Float::class.javaPrimitiveType -> Float::class.javaObjectType
            Double::class.javaPrimitiveType -> Double::class.javaObjectType
            Char::class.javaPrimitiveType -> Char::class.javaObjectType
            else -> this
        }

    private val integerTypes = setOf(
        Byte::class.javaObjectType,
        Short::class.javaObjectType,
        Int::class.javaObjectType,
        Long::class.javaObjectType,
    )

    private fun expectedType(type: Class<*>): ExpectedType {
        val normalizedType = type.boxed()

        return when {
            CharSequence::class.java.isAssignableFrom(normalizedType) ->
                ExpectedType.STRING
            normalizedType == Boolean::class.javaObjectType ->
                ExpectedType.BOOLEAN
            normalizedType in integerTypes ->
                ExpectedType.INTEGER
            Number::class.java.isAssignableFrom(normalizedType) ->
                ExpectedType.NUMBER
            normalizedType.isArray || Collection::class.java.isAssignableFrom(normalizedType) ->
                ExpectedType.ARRAY
            else ->
                ExpectedType.OBJECT
        }
    }

    fun sorted(violations: List<ValidationViolation>): List<ValidationViolation> =
        violations.sortedWith(compareBy({ it.field }, { it.reason.code }))

    private fun size(field: String, value: Any?, min: Int, max: Int): ValidationViolation {
        val itemBased = when {
            value is CharSequence -> false
            value is Collection<*> ||
                    value is Map<*, *> ||
                    value?.javaClass?.isArray == true -> true
            else -> return invalid(field)
        }

        return when {
            min == max -> ExactValidationViolation(
                field,
                if (itemBased) ExactValidationReason.ITEMS_EXACT
                else ExactValidationReason.LENGTH_EXACT,
                min.toLong(),
            )
            min != 0 && max < Int.MAX_VALUE -> BetweenValidationViolation(
                field,
                if (itemBased) BetweenValidationReason.ITEMS_BETWEEN
                else BetweenValidationReason.LENGTH_BETWEEN,
                min.toLong(),
                max.toLong(),
            )
            min != 0 -> MinimumValidationViolation(
                field,
                if (itemBased) MinimumValidationReason.ITEMS_MIN
                else MinimumValidationReason.LENGTH_MIN,
                min.toLong(),
            )
            else -> MaximumValidationViolation(
                field,
                if (itemBased) MaximumValidationReason.ITEMS_MAX
                else MaximumValidationReason.LENGTH_MAX,
                max.toLong(),
            )
        }
    }
}
