package xyz.om3lette.deadlines_api.data.notifications.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import xyz.om3lette.deadlines_api.data.notifications.converters.NotificationStatusConverter
import xyz.om3lette.deadlines_api.data.notifications.enums.TimeRemaining
import xyz.om3lette.deadlines_api.data.notifications.enums.NotificationStatus
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import java.time.Instant

@Entity
@Table(name = "deadline_notifications")
data class DeadlineNotification(
    @Id
    @SequenceGenerator(name = "notification_seq", sequenceName = "notification_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq")
    val id: Long,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deadline_id")
    val deadline: Deadline,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val sendAt: Instant,

    @Enumerated
    val type: TimeRemaining,

    @Convert(converter = NotificationStatusConverter::class)
    val status: NotificationStatus
)