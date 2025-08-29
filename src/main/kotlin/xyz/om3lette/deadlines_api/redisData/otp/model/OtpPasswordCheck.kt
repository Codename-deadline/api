package xyz.om3lette.deadlines_api.redisData.otp.model

import jakarta.persistence.Id
import org.springframework.data.redis.core.RedisHash
import java.util.UUID

@RedisHash("password_checks", timeToLive = 5 * 60 * 60)
data class OtpPasswordCheck(
    @Id
    val id: UUID = UUID.randomUUID(),

    val username: String,

    var attempts: Int = 0
)
