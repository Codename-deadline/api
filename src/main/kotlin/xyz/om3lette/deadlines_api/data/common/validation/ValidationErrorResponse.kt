package xyz.om3lette.deadlines_api.data.common.validation

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode

data class ValidationErrorResponse(
    @field:Schema(allowableValues = ["validation.failed"])
    val code: ErrorCode = ErrorCode.VALIDATION_FAILED,
    val violations: List<ValidationViolation>
)

@Schema(
    discriminatorProperty = "reason",
    oneOf = [
        SimpleValidationViolation::class,
        MinimumValidationViolation::class,
        MaximumValidationViolation::class,
        BetweenValidationViolation::class,
        ExactValidationViolation::class,
        OneOfValidationViolation::class,
        TypeValidationViolation::class
    ],
    discriminatorMapping = [
        DiscriminatorMapping(value = "required", schema = SimpleValidationViolation::class),
        DiscriminatorMapping(value = "not-blank", schema = SimpleValidationViolation::class),
        DiscriminatorMapping(value = "value.positive", schema = SimpleValidationViolation::class),
        DiscriminatorMapping(value = "invalid", schema = SimpleValidationViolation::class),
        DiscriminatorMapping(value = "format.digits-only", schema = SimpleValidationViolation::class),
        DiscriminatorMapping(value = "format.uuid", schema = SimpleValidationViolation::class),
        DiscriminatorMapping(value = "format.timestamp", schema = SimpleValidationViolation::class),
        DiscriminatorMapping(value = "length.min", schema = MinimumValidationViolation::class),
        DiscriminatorMapping(value = "items.min", schema = MinimumValidationViolation::class),
        DiscriminatorMapping(value = "value.min", schema = MinimumValidationViolation::class),
        DiscriminatorMapping(value = "length.max", schema = MaximumValidationViolation::class),
        DiscriminatorMapping(value = "items.max", schema = MaximumValidationViolation::class),
        DiscriminatorMapping(value = "value.max", schema = MaximumValidationViolation::class),
        DiscriminatorMapping(value = "length.between", schema = BetweenValidationViolation::class),
        DiscriminatorMapping(value = "items.between", schema = BetweenValidationViolation::class),
        DiscriminatorMapping(value = "length.exact", schema = ExactValidationViolation::class),
        DiscriminatorMapping(value = "items.exact", schema = ExactValidationViolation::class),
        DiscriminatorMapping(value = "value.one-of", schema = OneOfValidationViolation::class),
        DiscriminatorMapping(value = "type.invalid", schema = TypeValidationViolation::class)
    ]
)
sealed interface ValidationViolation {
    val field: String
    val reason: ValidationReason
}

data class SimpleValidationViolation(
    override val field: String,
    override val reason: SimpleValidationReason
) : ValidationViolation

data class MinimumValidationViolation(
    override val field: String,
    override val reason: MinimumValidationReason,
    val min: Long
) : ValidationViolation

data class MaximumValidationViolation(
    override val field: String,
    override val reason: MaximumValidationReason,
    val max: Long
) : ValidationViolation

data class BetweenValidationViolation(
    override val field: String,
    override val reason: BetweenValidationReason,
    val min: Long,
    val max: Long
) : ValidationViolation

data class ExactValidationViolation(
    override val field: String,
    override val reason: ExactValidationReason,
    val exact: Long
) : ValidationViolation

data class OneOfValidationViolation(
    override val field: String,
    override val reason: OneOfValidationReason = OneOfValidationReason.VALUE_ONE_OF,
    val allowed: List<String>
) : ValidationViolation

data class TypeValidationViolation(
    override val field: String,
    override val reason: TypeValidationReason = TypeValidationReason.TYPE_INVALID,
    val expected: ExpectedType
) : ValidationViolation

sealed interface ValidationReason {
    val code: String
}

enum class SimpleValidationReason(@get:JsonValue override val code: String) : ValidationReason {
    REQUIRED("required"),
    NOT_BLANK("not-blank"),
    VALUE_POSITIVE("value.positive"),
    INVALID("invalid"),
    FORMAT_DIGITS_ONLY("format.digits-only"),
    FORMAT_UUID("format.uuid"),
    FORMAT_TIMESTAMP("format.timestamp")
}

enum class MinimumValidationReason(@get:JsonValue override val code: String) : ValidationReason {
    LENGTH_MIN("length.min"),
    ITEMS_MIN("items.min"),
    VALUE_MIN("value.min")
}

enum class MaximumValidationReason(@get:JsonValue override val code: String) : ValidationReason {
    LENGTH_MAX("length.max"),
    ITEMS_MAX("items.max"),
    VALUE_MAX("value.max")
}

enum class BetweenValidationReason(@get:JsonValue override val code: String) : ValidationReason {
    LENGTH_BETWEEN("length.between"),
    ITEMS_BETWEEN("items.between")
}

enum class ExactValidationReason(@get:JsonValue override val code: String) : ValidationReason {
    LENGTH_EXACT("length.exact"),
    ITEMS_EXACT("items.exact")
}

enum class OneOfValidationReason(@get:JsonValue override val code: String) : ValidationReason {
    VALUE_ONE_OF("value.one-of")
}

enum class TypeValidationReason(@get:JsonValue override val code: String) : ValidationReason {
    TYPE_INVALID("type.invalid")
}

enum class ExpectedType(@get:JsonValue val code: String) {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    OBJECT("object"),
    ARRAY("array")
}
