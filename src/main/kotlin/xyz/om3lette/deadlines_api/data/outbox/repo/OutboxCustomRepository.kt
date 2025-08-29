package xyz.om3lette.deadlines_api.data.outbox.repo

import xyz.om3lette.deadlines_api.data.outbox.model.Outbox

interface OutboxCustomRepository {
    fun claimBatch(batchSize: Int, maxRetries: Int): List<Outbox>
}