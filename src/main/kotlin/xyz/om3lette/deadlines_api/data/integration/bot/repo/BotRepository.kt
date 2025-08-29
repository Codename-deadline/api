package xyz.om3lette.deadlines_api.data.integration.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.bot.model.Bot
import java.util.Optional

interface BotRepository : JpaRepository<Bot, Long> {
    fun findByBotIdAndMessenger(botId: Long, messenger: Messenger): Optional<Bot>
}