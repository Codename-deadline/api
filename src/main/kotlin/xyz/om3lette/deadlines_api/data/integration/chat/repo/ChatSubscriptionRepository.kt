package xyz.om3lette.deadlines_api.data.integration.chat.repo

import org.springframework.data.jpa.repository.JpaRepository
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.chat.model.ChatSubscription
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType

interface ChatSubscriptionRepository : JpaRepository<ChatSubscription, Long> {
    fun deleteByChatAndScopeIdAndScopeType(chat: Chat, scopeId: Long, scopeType: ScopeType): Int

    fun deleteAllByChatAndScopeTypeAndScopeIdIn(chat: Chat, scopeType: ScopeType, scopeIds: Collection<Long>): Int

    fun deleteAllByChat(chat: Chat): Int
}
