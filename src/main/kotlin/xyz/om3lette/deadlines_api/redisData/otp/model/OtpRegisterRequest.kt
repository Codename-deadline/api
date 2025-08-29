package xyz.om3lette.deadlines_api.redisData.otp.model

import jakarta.persistence.Id
import org.springframework.data.redis.core.RedisHash
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import java.util.UUID

@RedisHash("register_requests", timeToLive = 5 * 60 * 60)
data class OtpRegisterRequest(
    @Id
    val id: UUID = UUID.randomUUID(),

    val username: String,

    val fullName: String,

    val language: Language?,

    val identifier: String,

    val channel: OtpChannel
)
