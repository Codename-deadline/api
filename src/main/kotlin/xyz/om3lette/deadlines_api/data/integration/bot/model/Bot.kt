package xyz.om3lette.deadlines_api.data.integration.bot.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import xyz.om3lette.deadlines_api.data.integration.constraints.IntegrationConstraints
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger

@Entity
@Table(
    name = "bots",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_bots_messenger", columnNames = ["messenger"])
    ]
)
data class Bot(
    @Id
    @GeneratedValue
    val id: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val messenger: Messenger,

    @Column(nullable = false)
    val botId: Long,

    @field:Size(max = IntegrationConstraints.BOT_USERNAME_MAX)
    @Column(length = IntegrationConstraints.BOT_USERNAME_MAX, nullable = false)
    val username: String,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "bot")
    @OnDelete(action = OnDeleteAction.CASCADE)
    val chats: MutableList<Chat>
)
