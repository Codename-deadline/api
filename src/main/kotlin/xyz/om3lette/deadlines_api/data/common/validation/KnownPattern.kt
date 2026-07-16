package xyz.om3lette.deadlines_api.data.common.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import xyz.om3lette.deadlines_api.data.common.validation.enums.KnownPatternReason
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [KnownPatternValidator::class])
annotation class KnownPattern(
    val regexp: String,
    val reason: KnownPatternReason,
    val message: String = "{validation.known-pattern}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class KnownPatternValidator : ConstraintValidator<KnownPattern, CharSequence?> {
    private lateinit var pattern: Regex

    override fun initialize(annotation: KnownPattern) {
        pattern = Regex(annotation.regexp)
    }

    override fun isValid(value: CharSequence?, context: ConstraintValidatorContext): Boolean =
        value == null || pattern.matches(value)
}
