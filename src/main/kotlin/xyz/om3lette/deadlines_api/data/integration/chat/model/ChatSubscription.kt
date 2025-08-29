package xyz.om3lette.deadlines_api.data.integration.chat.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import xyz.om3lette.deadlines_api.data.scopes.userScope.converters.ScopeTypeConverter
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import java.time.Instant

@Entity
@Table(
    name = "chat_subscriptions",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["chat_id", "scope_id"])
    ]
)
data class ChatSubscription(
    @Id
    @SequenceGenerator(name = "chat_sub_seq", sequenceName = "chat_sub_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_sub_seq")
    val id: Long,

    @ManyToOne
    @JoinColumn(nullable = false)
    val chat: Chat,

    @Column(nullable = false)
    val scopeId: Long,

    @Convert(converter = ScopeTypeConverter::class)
    val scopeType: ScopeType,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val subscribedAt: Instant
)