package xyz.om3lette.deadlines_api.redisData.otp.model

import jakarta.persistence.Id
import org.springframework.data.redis.core.RedisHash
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpPurpose
import java.util.UUID

@RedisHash(value = "otps", timeToLive = 5 * 60 * 60)
data class Otp(
    @Id
    val id: UUID = UUID.randomUUID(),

    val hashedCode: String,

    val purpose: OtpPurpose,

    /**
     * Purpose-specific context. SIGN_IN stores username; REGISTRATION stores OtpRegisterRequest id.
     */
    val context: String,

    var attempts: Int = 0
)
