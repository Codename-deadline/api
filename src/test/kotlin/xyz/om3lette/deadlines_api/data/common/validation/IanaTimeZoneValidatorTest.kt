package xyz.om3lette.deadlines_api.data.common.validation

import io.mockk.mockk
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IanaTimeZoneValidatorTest {
    private val validator = IanaTimeZoneValidator()
    private val context: ConstraintValidatorContext = mockk()

    @ParameterizedTest
    @ValueSource(strings = ["Etc/UTC", "Europe/Paris", "America/New_York"])
    fun `accepts JVM IANA timezone IDs`(timeZone: String) {
        assertTrue(validator.isValid(timeZone, context))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "+02:00", "Europe/Nowhere"])
    fun `rejects blanks raw offsets and unknown timezone IDs`(timeZone: String) {
        assertFalse(validator.isValid(timeZone, context))
    }

    @Test
    fun `rejects timezone IDs longer than the database column`() {
        assertFalse(validator.isValid("a".repeat(IanaTimeZones.MAX_LENGTH + 1), context))
    }

    @Test
    fun `allows null for optional request fields`() {
        assertTrue(validator.isValid(null, context))
    }
}
