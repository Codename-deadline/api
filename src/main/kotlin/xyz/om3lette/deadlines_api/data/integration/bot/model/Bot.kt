package xyz.om3lette.deadlines_api.data.integration.bot.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger

@Entity
@Table(
    name = "bots",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["bot_id", "messenger"])
    ]
)
data class Bot(
    @Id
    @GeneratedValue
    val id: Long,

    val messenger: Messenger,

    val botId: Long,

    @field:Size(max = 64)
    val username: String,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "bot")
    val chats: MutableList<Chat>
)
