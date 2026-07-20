package xyz.om3lette.deadlines_api.data.integration.chat.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import java.time.Instant

@Entity
@Table(name = "chat_subscriptions")
@IdClass(ChatSubscriptionId::class)
data class ChatSubscription(
    @Id
    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    val chat: Chat,

    @Id
    @Column(nullable = false)
    val scopeId: Long,

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 3, nullable = false)
    val scopeType: ScopeType,

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val subscribedAt: Instant
)
