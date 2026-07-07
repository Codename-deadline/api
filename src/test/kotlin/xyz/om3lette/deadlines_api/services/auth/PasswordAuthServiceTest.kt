package xyz.om3lette.deadlines_api.services.auth

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.data.jwt.dto.TokenPair
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class PasswordAuthServiceTest {
    private val authenticationManager: AuthenticationManager = mockk()
    private val authSessionService: AuthSessionService = mockk()
    private val passwordAuthService = PasswordAuthService(authenticationManager, authSessionService)

    @Test
    fun `signIn authenticates password and issues session`() {
        val user = DomainObjectBuilder.user(username = "bob")
        val tokenPair = TokenPair("access", "refresh")
        val auth = mockk<Authentication> {
            every { principal } returns user
        }
        every {
            authenticationManager.authenticate(
                match<UsernamePasswordAuthenticationToken> {
                    it.principal == "bob" && it.credentials == "raw-password"
                }
            )
        } returns auth
        every { authSessionService.issueSession(user) } returns tokenPair

        assertEquals(tokenPair, passwordAuthService.signIn("bob", "raw-password"))
    }
}