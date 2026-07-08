package xyz.om3lette.deadlines_api.services.auth.otp

import org.junit.jupiter.api.Test
import xyz.om3lette.deadlines_api.configs.properties.OtpProperties
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtpCodeHasherTest {
    private val otpCodeHasher = OtpCodeHasher(
        OtpProperties(hashSecret = "test-secret")
    )

    @Test
    fun `matches returns true for original code`() {
        val hash = otpCodeHasher.hash("123456")

        assertTrue(otpCodeHasher.matches("123456", hash))
    }

    @Test
    fun `matches returns false for different code`() {
        val hash = otpCodeHasher.hash("123456")

        assertFalse(otpCodeHasher.matches("654321", hash))
    }
}
