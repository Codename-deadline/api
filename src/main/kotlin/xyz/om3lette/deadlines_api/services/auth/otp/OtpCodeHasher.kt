package xyz.om3lette.deadlines_api.services.auth.otp

import org.springframework.stereotype.Component
import xyz.om3lette.deadlines_api.configs.properties.OtpProperties
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class OtpCodeHasher(
    otpProperties: OtpProperties
) {
    private val secretKey = SecretKeySpec(
        otpProperties.hashSecret.toByteArray(StandardCharsets.UTF_8),
        HMAC_ALGORITHM
    )

    fun hash(code: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(secretKey)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
            mac.doFinal(code.toByteArray(StandardCharsets.UTF_8))
        )
    }

    fun matches(code: String, hashedCode: String): Boolean = MessageDigest.isEqual(
        hash(code).toByteArray(StandardCharsets.UTF_8),
        hashedCode.toByteArray(StandardCharsets.UTF_8)
    )

    private companion object {
        const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
