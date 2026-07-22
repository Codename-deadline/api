package xyz.om3lette.deadlines_api.data.integration.messengerAccount.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.dto.MessengerAccountDTO
import xyz.om3lette.deadlines_api.data.user.model.User

@Entity
@Table(
    name = "user_messenger_accounts",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["account_id", "messenger"])
    ]
)
data class UserMessengerAccount (
    @GeneratedValue
    @Id
    val id: Long,

    @ManyToOne(fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val accountId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val messenger: Messenger
) {
    fun toResponse() = MessengerAccountDTO(
        accountId = accountId,
        messenger = messenger
    )
}
