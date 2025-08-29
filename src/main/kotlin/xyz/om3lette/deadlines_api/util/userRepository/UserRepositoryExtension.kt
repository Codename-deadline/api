package xyz.om3lette.deadlines_api.util.userRepository

import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException

fun UserRepository.findByUsernameIgnoreCaseOr404(username: String): User =
    findByUsernameIgnoreCase(username).orElseThrow {
        StatusCodeException(404, "User $username not found")
    }
