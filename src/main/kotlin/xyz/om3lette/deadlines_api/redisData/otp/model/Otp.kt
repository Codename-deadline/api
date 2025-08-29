package xyz.om3lette.deadlines_api.redisData.otp.model

import jakarta.persistence.Id
import org.springframework.data.redis.core.RedisHash
import java.util.UUID

@RedisHash(value = "otps", timeToLive = 5 * 60 * 60)
data class Otp(
    @Id
    val id: UUID = UUID.randomUUID(),

    val hashedCode: String,

    val registerRequestId: UUID? = null,

    val username: String? = null,

    var attempts: Int = 0
)
