package xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.model

import org.springframework.data.redis.core.RedisHash
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger

@RedisHash(value = "account_linkage_requests", timeToLive = 3600)
data class AccountLinkageRequest(
    val id: String,
    val accountId: Long,
    val messenger: Messenger,
    val userId: Long
)
