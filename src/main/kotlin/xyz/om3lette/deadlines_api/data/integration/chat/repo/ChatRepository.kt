package xyz.om3lette.deadlines_api.data.integration.chat.repo

import org.springframework.data.jpa.repository.JpaRepository
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import java.util.Optional

interface ChatRepository : JpaRepository<Chat, Long> {
    fun findByMessengerChatIdAndMessenger(messengerChatId: Long, messenger: Messenger): Chat?
}