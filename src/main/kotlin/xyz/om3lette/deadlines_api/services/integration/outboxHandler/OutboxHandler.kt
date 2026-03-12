package xyz.om3lette.deadlines_api.services.integration.outboxHandler

import tools.jackson.databind.JsonNode
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.outbox.enums.ProcessResult

interface OutboxHandler {
    val topic: String

    fun handle(messenger: Messenger, payload: JsonNode): ProcessResult
}
