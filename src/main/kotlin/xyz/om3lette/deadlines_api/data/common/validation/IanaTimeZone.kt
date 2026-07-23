package xyz.om3lette.deadlines_api.data.common.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.time.ZoneId
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [IanaTimeZoneValidator::class])
annotation class IanaTimeZone(
    val message: String = "{validation.iana-time-zone}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class IanaTimeZoneValidator : ConstraintValidator<IanaTimeZone, CharSequence?> {
    override fun isValid(value: CharSequence?, context: ConstraintValidatorContext): Boolean =
        value == null || IanaTimeZones.isValid(value)
}

object IanaTimeZones {
    const val MAX_LENGTH = 64
    const val DEFAULT = "Etc/UTC"

    private val availableIds = ZoneId.getAvailableZoneIds()

    fun isValid(value: CharSequence): Boolean =
        value.isNotBlank() && value.length <= MAX_LENGTH && value.toString() in availableIds
}
