package xyz.om3lette.deadlines_api.data.notifications.repo.impl

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.notifications.repo.DeadlineNotificationCustomRepository

@Repository
class DeadlineNotificationCustomRepositoryImpl(
    @PersistenceContext private val em: EntityManager
) : DeadlineNotificationCustomRepository {

    @Transactional
    override fun findNotificationRecipientsAndInsertIntoOutbox(batchSize: Int) {
        val sql = """
            WITH due AS (
                SELECT dn.id
                FROM deadline_notifications dn
                WHERE dn.send_at <= now() AND dn.status = 'P'
                ORDER BY dn.send_at, dn.id
                LIMIT :batch_size
                FOR UPDATE OF dn SKIP LOCKED
            ),
            moved AS (
                UPDATE deadline_notifications dn
                SET status = 'I'
                FROM due
                WHERE dn.id = due.id
                RETURNING dn.id AS notification_id, dn.type, dn.deadline_id
            ),
            notification_context AS (
                SELECT
                    m.notification_id,
                    m.type,
                    d.id AS deadline_id,
                    d.title AS deadline_title,
                    d.due,
                    d.is_completed,
                    t.id AS thread_id,
                    t.title AS thread_title,
                    o.id AS organization_id,
                    o.title AS organization_title
                FROM moved m
                JOIN deadlines d ON d.id = m.deadline_id
                JOIN threads t ON t.id = d.thread_id
                JOIN organizations o ON o.id = t.organization_id
            ),
            candidates AS (
                SELECT cs.chat_id,
                       nc.*,
                       1 AS precedence
                FROM notification_context nc
                 JOIN chat_subscriptions cs ON cs.scope_type = 'DEADLINE' AND cs.scope_id = nc.deadline_id
                WHERE nc.is_completed = false
                
                UNION ALL
                
                SELECT cs.chat_id,
                       nc.*,
                       2 AS precedence
                FROM notification_context nc
                 JOIN chat_subscriptions cs ON cs.scope_type = 'THREAD' AND cs.scope_id = nc.thread_id
                WHERE nc.is_completed = false
                
                UNION ALL
                
                SELECT cs.chat_id,
                       nc.*,
                       3 AS precedence
                FROM notification_context nc
                 JOIN chat_subscriptions cs ON cs.scope_type = 'ORGANIZATION' AND cs.scope_id = nc.organization_id
                WHERE nc.is_completed = false
            ),
            selected AS (
                SELECT
                    notification_id,
                    chat_id,
                    type,
                    deadline_id,
                    deadline_title,
                    due,
                    thread_id,
                    thread_title,
                    organization_id,
                    organization_title
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
                    messenger_chat_id,
                    type,
                    deadline_id,
                    deadline_title,
                    due,
                    thread_id,
                    thread_title,
                    organization_id,
                    organization_title,
                    messenger,
                    language
                FROM selected s
                JOIN chats c ON c.id = s.chat_id
            ),
            inserted AS (
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
                    'DDL_EXPIRATION',
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
                        'organization', jsonb_build_object(
                            'id', organization_id,
                            'title', organization_title
                        ),
                        'thread', jsonb_build_object(
                            'id', thread_id,
                            'title', thread_title
                        ),
                        'deadline', jsonb_build_object(
                            'id', deadline_id,
                            'title', deadline_title,
                            'due', due
                        )
                    )
                FROM selected_with_messenger
                RETURNING notification_id
            )
            UPDATE deadline_notifications dn
            SET status = 'S'
            FROM moved m
            WHERE dn.id = m.notification_id
                AND NOT EXISTS (
                    SELECT 1
                    FROM inserted i
                    WHERE i.notification_id = m.notification_id
                )
        """.trimIndent()

        em.createNativeQuery(sql)
            .setParameter("batch_size", batchSize)
            .executeUpdate()
    }

    @Transactional
    override fun finalizeProcessedNotifications(notificationIds: Collection<Long>) {
        if (notificationIds.isEmpty()) return

        val sql = """
            UPDATE deadline_notifications dn
            SET status = CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM notification_outbox no
                    WHERE no.notification_id = dn.id AND no.status = 'F'
                ) THEN 'F'
                ELSE 'S'
            END
            WHERE dn.id IN (:notification_ids)
                AND dn.status = 'I'
                AND NOT EXISTS (
                    SELECT 1
                    FROM notification_outbox no
                    WHERE no.notification_id = dn.id AND no.status IN ('P', 'I')
                )
        """.trimIndent()

        em.createNativeQuery(sql)
            .unwrap(org.hibernate.query.NativeQuery::class.java)
            .setParameterList("notification_ids", notificationIds)
            .executeUpdate()
    }
}
