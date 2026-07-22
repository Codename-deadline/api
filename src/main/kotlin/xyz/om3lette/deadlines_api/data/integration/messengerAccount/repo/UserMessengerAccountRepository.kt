package xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.dto.MessengerAccountDTO
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.user.model.User
import java.util.Optional

interface UserMessengerAccountRepository : JpaRepository<UserMessengerAccount, Long> {
//    Do not use `findByAccountId`, always use findByMessengerAndAccountId
//    As the unique constraints are (messenger, accountId) not accountId
//    fun findByAccountId(accountId: Long): Optional<UserMessengerAccount>

    fun existsByAccountId(accountId: Long): Boolean

    fun findByMessengerAndAccountId(messenger: Messenger, accountId: Long): Optional<UserMessengerAccount>

    fun findAllByUserAndMessenger(user: User, messenger: Messenger): List<UserMessengerAccount>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uma FROM UserMessengerAccount uma WHERE uma.user = :user ORDER BY uma.id")
    fun findAllByUserForUpdate(user: User): List<UserMessengerAccount>


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

    @Query("""
        SELECT uma.accountId, uma.messenger FROM UserMessengerAccount uma
        WHERE uma.user.id = :userId
        ORDER BY uma.messenger
    """)
    fun findAllDTOByUserAndSortByMessenger(userId: Long): List<MessengerAccountDTO>

    fun deleteByUserAndAccountIdAndMessenger(user: User, accountId: Long, messenger: Messenger): Long
}
