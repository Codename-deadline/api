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

    val notificationId: Long,

    @Enumerated(EnumType.STRING)
    val source: OutboxSource,

    @Enumerated(EnumType.STRING)
    val messenger: Messenger,

    val priority: Int,

    val topic: String,

    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb")
    val payload: JsonNode,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    var availableAt: Instant = Instant.now(),

    @Convert(converter = NotificationStatusConverter::class)
    var status: NotificationStatus = NotificationStatus.PENDING,

    var retries: Int = 0,
)
