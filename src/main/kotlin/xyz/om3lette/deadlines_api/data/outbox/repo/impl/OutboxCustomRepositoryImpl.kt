package xyz.om3lette.deadlines_api.data.outbox.repo.impl

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import xyz.om3lette.deadlines_api.data.outbox.model.Outbox
import xyz.om3lette.deadlines_api.data.outbox.repo.OutboxCustomRepository

class OutboxCustomRepositoryImpl(
    @PersistenceContext private val em: EntityManager
) : OutboxCustomRepository {
    override fun claimBatch(batchSize: Int, maxRetries: Int): List<Outbox> {
        val sql = """
            UPDATE notification_outbox
            SET status = 'I',
                retries = retries + 1,
                available_at = available_at + interval '2 minutes'
            WHERE id IN (
                SELECT id
                FROM notification_outbox
                WHERE available_at <= now()
                    AND status != 'F'
                    AND retries < :max_retries
                ORDER BY priority DESC, retries ASC, available_at ASC
                LIMIT :batch_size
            )
            RETURNING *;
        """.trimIndent()

        val q = em.createNativeQuery(sql, Outbox::class.java)
            .setParameter("batch_size", batchSize)
            .setParameter("max_retries", maxRetries)

        @Suppress("UNCHECKED_CAST")
        return q.resultList as List<Outbox>
    }
}