package xyz.om3lette.deadlines_api.data.integration.chat.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size
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

    val messengerChatId: Long,

    val messenger: Messenger,

    @field:Size(max = 256)
    var title: String,

    @ManyToOne(fetch = FetchType.EAGER)
    val bot: Bot,

    @Enumerated(EnumType.STRING)
    var language: Language = Language.RU,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val registeredAt: Instant,

    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        mappedBy = "chat"
    )
    val subscriptions: MutableList<ChatSubscription> = mutableListOf(),
)