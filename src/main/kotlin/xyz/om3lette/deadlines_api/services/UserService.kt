package xyz.om3lette.deadlines_api.services

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.dto.MessengerAccountDTO
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.data.user.request.PatchUserRequest
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val messengerAccountRepository: UserMessengerAccountRepository
) {
    fun getUsernamesStartingWith(usernameStart: String): List<String> =
        userRepository.findUsernamesStartingWithIgnoreCase(
            usernameStart.lowercase(), Pageable.ofSize(10)
        )

    fun deleteUser(user: User) = userRepository.delete(user)

    fun patchUser(user: User, username: String?, fullName: String?, language: Language?) {
        if (!username.isNullOrBlank()) {
            if (!username.equals(user.username, ignoreCase = true) && userRepository.existsByUsernameIgnoreCase(username)) {
                throw StatusCodeException(409, ErrorCode.USER_ALREADY_EXISTS)
            }
            user._username = username
        }
        if (!fullName.isNullOrBlank()) user.fullName = fullName
        if (language != null) user.language = language

        userRepository.save(user)
    }

    fun getMessengerAccounts(user: User): List<MessengerAccountDTO>
        = messengerAccountRepository.findAllDTOByUserAndSortByMessenger(user.id)
}
