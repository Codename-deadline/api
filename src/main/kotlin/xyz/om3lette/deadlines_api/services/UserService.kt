package xyz.om3lette.deadlines_api.services

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun getUsernamesStartingWith(usernameStart: String): List<String> =
        userRepository.findUsernamesStartingWithIgnoreCase(
            usernameStart, Pageable.ofSize(10)
        )
}
