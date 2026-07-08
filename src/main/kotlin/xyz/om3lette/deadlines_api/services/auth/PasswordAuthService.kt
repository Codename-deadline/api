package xyz.om3lette.deadlines_api.services.auth

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.jwt.dto.TokenPair
import xyz.om3lette.deadlines_api.data.user.model.User

@Service
class PasswordAuthService(
    private val authenticationManager: AuthenticationManager,
    private val authSessionService: AuthSessionService
) {
    fun signIn(username: String, password: String): TokenPair {
        val auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(username, password)
        )

        return authSessionService.issueSession(auth.principal as User)
    }
}
