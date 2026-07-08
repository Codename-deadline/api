package xyz.om3lette.deadlines_api.services.auth

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import java.time.Instant

@Service
class AccountSecurityService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val authSessionService: AuthSessionService
) {
    fun changePassword(user: User, oldPassword: String?, newPassword: String) {
        if (oldPassword == newPassword) {
            throw StatusCodeException(400, ErrorCode.PASSWORD_CHANGE_UNCHANGED)
        }

        if (user.password == null) {
            if (oldPassword != null) {
                throw StatusCodeException(403, ErrorCode.PASSWORD_CHANGE_INVALID_CREDENTIALS)
            }
        } else if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.password)) {
            throw StatusCodeException(403, ErrorCode.PASSWORD_CHANGE_INVALID_CREDENTIALS)
        }

        user._password = passwordEncoder.encode(newPassword)
        user.lastPasswordChange = Instant.now()
        userRepository.save(user)

        authSessionService.revokeAllSessions(user)
    }
}
