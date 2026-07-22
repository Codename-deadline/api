package xyz.om3lette.deadlines_api.data.integration.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import xyz.om3lette.deadlines_api.data.integration.bot.dto.BotDTO
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.bot.model.Bot
import java.util.Optional

interface BotRepository : JpaRepository<Bot, Long> {
    fun findByBotIdAndMessenger(botId: Long, messenger: Messenger): Optional<Bot>

    @Query("""
        SELECT
            b.botId as botId,
            b.username as username,
            b.messenger as messenger
        FROM Bot b  
        ORDER BY b.messenger
    """)
    fun findAllDTOSortByMessenger(): List<BotDTO>
}
