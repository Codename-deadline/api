package xyz.om3lette.deadlines_api.data.outbox.model

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import tools.jackson.databind.JsonNode
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.notifications.converters.NotificationStatusConverter
import xyz.om3lette.deadlines_api.data.notifications.enums.NotificationStatus
import xyz.om3lette.deadlines_api.data.outbox.enums.OutboxSource
import java.time.Instant

@Entity
@Table(name = "notification_outbox")
data class Outbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(nullable = false)
    val notificationId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val source: OutboxSource,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val messenger: Messenger,

    @Column(nullable = false)
    val priority: Int,

    @Column(nullable = false)
    val topic: String,

    @Type(JsonType::class)
    @Column(nullable = false, columnDefinition = "jsonb")
    val payload: JsonNode,

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    var availableAt: Instant = Instant.now(),

    @Convert(converter = NotificationStatusConverter::class)
    @Column(nullable = false, length = 1)
    var status: NotificationStatus = NotificationStatus.PENDING,

    @Column(nullable = false)
    var retries: Int = 0,
)
