package xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.user.model.User
import java.util.Optional

interface UserMessengerAccountRepository : JpaRepository<UserMessengerAccount, Long> {
//    Do not use `findByAccountId`, always use findByMessengerAndAccountId
//    As the unique constraints are (messenger, accountId) not accountId
//    fun findByAccountId(accountId: Long): Optional<UserMessengerAccount>

    fun findByMessengerAndAccountId(messenger: Messenger, accountId: Long): Optional<UserMessengerAccount>

    fun findAllByUserAndMessenger(user: User, messenger: Messenger): List<UserMessengerAccount>


    @Query("""
        SELECT uma FROM UserMessengerAccount uma
        WHERE LOWER(uma.user._username) = LOWER(:username)
            AND uma.messenger = :messenger
            AND uma.accountId = :accountId
    """)
    fun findAccountByUsernameAndMessengerAndAccountId(
        username: String,
        messenger: Messenger,
        accountId: Long
    ): UserMessengerAccount?
}
