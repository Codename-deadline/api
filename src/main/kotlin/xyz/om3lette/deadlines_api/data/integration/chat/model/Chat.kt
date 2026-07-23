package xyz.om3lette.deadlines_api.data.integration.chat.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import xyz.om3lette.deadlines_api.data.common.validation.IanaTimeZone
import xyz.om3lette.deadlines_api.data.common.validation.IanaTimeZones
import xyz.om3lette.deadlines_api.data.integration.constraints.IntegrationConstraints
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.bot.model.Bot
import java.time.Instant

@Entity
@Table(
    name = "chats",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["messenger_chat_id", "messenger"])
    ]
)
data class Chat(
    @Id
    @GeneratedValue
    val id: Long,

    @Column(nullable = false)
    val messengerChatId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val messenger: Messenger,

    @field:Size(max = IntegrationConstraints.CHAT_TITLE_MAX)
    @Column(length = IntegrationConstraints.CHAT_TITLE_MAX, nullable = false)
    var title: String,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bot_id", nullable = false)
    val bot: Bot,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    var language: Language = Language.RU,

    @field:IanaTimeZone
    @Column(nullable = false, length = IanaTimeZones.MAX_LENGTH)
    var timeZone: String,

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val registeredAt: Instant,

    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        mappedBy = "chat"
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    val subscriptions: MutableList<ChatSubscription> = mutableListOf(),
)
