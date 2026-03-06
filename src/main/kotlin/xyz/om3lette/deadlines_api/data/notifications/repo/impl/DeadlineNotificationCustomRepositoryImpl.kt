package xyz.om3lette.deadlines_api.data.notifications.repo.impl

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository
import xyz.om3lette.deadlines_api.data.notifications.repo.DeadlineNotificationCustomRepository

@Repository
class DeadlineNotificationCustomRepositoryImpl(
    @PersistenceContext private val em: EntityManager
) : DeadlineNotificationCustomRepository {

    @Transactional
    override fun findNotificationRecipientsAndInsertIntoOutbox(batchSize: Int) {
        val sql = """
            WITH due AS (
                SELECT id
                FROM deadline_notifications
                WHERE send_at <= now() AND status = 'P'
                ORDER BY type
                LIMIT :batch_size
                FOR UPDATE SKIP LOCKED
            ),
            moved AS (
                UPDATE deadline_notifications dn
                SET status = 'I'
                FROM due
                WHERE dn.id = due.id
                RETURNING dn.id, dn.type, dn.deadline_id
            ),
            candidates AS (
                SELECT cs.chat_id,
                       m.id AS notification_id, m.type,
                       d.id AS deadline_id, d.title, d.due,
                       1 AS precedence
                FROM moved m
                JOIN deadlines d ON d.id = m.deadline_id
                JOIN chat_subscriptions cs ON cs.scope_type = 'DDL' AND cs.scope_id = d.id
                
                UNION ALL
                
                SELECT cs.chat_id,
                       m.id, m.type,
                       d.id, d.title, d.due,
                       2 AS precedence
                FROM moved m
                JOIN deadlines d ON d.id = m.deadline_id
                JOIN chat_subscriptions cs ON cs.scope_type = 'THR' AND cs.scope_id = d.thread_id
                
                UNION ALL
                
                SELECT cs.chat_id,
                       m.id, m.type,
                       d.id, d.title, d.due,
                       3 AS precedence
                FROM moved m
                JOIN deadlines d ON d.id = m.deadline_id
                JOIN chat_subscriptions cs ON cs.scope_type = 'ORG' AND cs.scope_id = d.organization_id
            ),
            selected AS (
                SELECT
                    notification_id, deadline_id, chat_id, type, title as deadline_title, due
                FROM (
                    SELECT *,
                        ROW_NUMBER() OVER (PARTITION BY chat_id, notification_id ORDER BY precedence) AS rn
                    FROM candidates
                ) t
                WHERE rn = 1
            ),
            selected_with_messenger AS (
                SELECT
                    notification_id,
                    deadline_id,
                    messenger_chat_id,
                    type,
                    deadline_title,
                    due,
                    messenger,
                    language
                FROM selected s
                JOIN chats c ON c.id = s.chat_id
            )
            INSERT INTO notification_outbox (
                notification_id,
                source,
                messenger,
                available_at,
                status,
                retries,
                priority,
                topic,
                payload
            )
            SELECT
                notification_id,
                0,
                messenger,
                now(),
                'P',
                0,
                100,
                'private.integration.notifications',
                jsonb_build_object(
                    'chatId', messenger_chat_id,
                    'timeRemaining', type,
                    'language', language,
                    'deadline', jsonb_build_object(
                        'id', deadline_id,
                        'title', deadline_title,
                        'due', due
                    )
                )
            FROM selected_with_messenger
        """.trimIndent()

        em.createNativeQuery(sql)
            .setParameter("batch_size", batchSize)
            .executeUpdate()
    }
}