package xyz.om3lette.deadlines_api.data.integration.chat.repo

import org.springframework.data.jpa.repository.JpaRepository
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.chat.model.ChatSubscription

interface ChatSubscriptionRepository : JpaRepository<ChatSubscription, Long> {
    fun deleteByChatAndScopeId(chat: Chat, scopeId: Long): Boolean

    fun deleteAllByChat(chat: Chat): Int
}