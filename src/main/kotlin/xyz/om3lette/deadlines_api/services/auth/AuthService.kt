package xyz.om3lette.deadlines_api.services.auth

import io.jsonwebtoken.Claims
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.jwt.model.RefreshToken
import xyz.om3lette.deadlines_api.data.jwt.repo.RefreshTokenRepository
import xyz.om3lette.deadlines_api.data.jwt.dto.TokenPair
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.JwtService
import java.time.Instant

@Service
class AuthService(
    @param:Value("\${users.max-sessions}") private val maxSessions: Int,
    private val jwtService: JwtService,
    val authenticationManager: AuthenticationManager,
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun registerWithPassword(
        username: String,
        fullName: String,
        password: String,
        language: Language?
    ) {
        try {
            userRepository.save(
                User(
                    0,
                    username,
                    Instant.now(),
                    fullName,
                    passwordEncoder.encode(password),
                    language ?: Language.EN
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw StatusCodeException(409, "User already exists")
        }
    }

//    Can be used with external auth checks e.g. OTP
    fun signInNoPasswordCheck(user: User): TokenPair {
        val openedSessions = refreshTokenRepository.findAllValidByUser(user).count()

        if (openedSessions >= maxSessions) {
            throw StatusCodeException(400, "Sessions limit reached: $openedSessions")
        }

        return generateTokenPair(user)
    }

    fun signInPassword(username: String, password: String): TokenPair {
        val auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(username, password)
        )

        val user = auth.principal as User
        return signInNoPasswordCheck(user)
    }

    fun signOut(user: User) =
        invalidateRefreshTokensByUser(user)

    private fun invalidateRefreshTokensByUser(user: User) {
        val userValidTokens = refreshTokenRepository.findAllValidByUser(user)

        userValidTokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(userValidTokens)
    }

    fun changePassword(user: User, oldPassword: String?, newPassword: String) {
        if (oldPassword == newPassword) {
            throw StatusCodeException(400, "New password must not match the old one")
        }

        if (
            !(oldPassword == null && user._password == null) &&
            !passwordEncoder.matches(oldPassword, user.password)
        ) {
            throw StatusCodeException(403, "Invalid password")
        }

        user._password = passwordEncoder.encode(newPassword)
        user.lastPasswordChange = Instant.now()
        userRepository.save(user)

        invalidateRefreshTokensByUser(user)
    }

    fun refreshToken(request: HttpServletRequest): TokenPair {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw StatusCodeException(401, "Invalid header")
        }

        val jwt = authHeader.substring(7)

        val claims: Claims?
        try {
            claims = jwtService.extractAllClaims(jwt)
        } catch (_: Exception) {
            throw StatusCodeException(401, "Invalid credentials")
        }

        val username = claims.subject
        val jti = claims["jti"] as String?

        if (username == null || jti == null) {
            throw StatusCodeException(401, "Invalid credentials")
        }

        val user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow { StatusCodeException(401, "Invalid credentials") }
        val refreshTokenEntry = refreshTokenRepository.findByJti(jti)
            .orElseThrow { StatusCodeException(401, "Invalid credentials") }

        if (refreshTokenEntry.revoked) {
            throw StatusCodeException(401, "Invalid credentials")
        }

        refreshTokenEntry.revoked = true
        refreshTokenRepository.save(refreshTokenEntry)

        return generateTokenPair(user)
    }


    private fun generateTokenPair(user: User): TokenPair {
        val accessTokenData = jwtService.generateAccessToken(user)
        val refreshTokenData = jwtService.generateRefreshToken(user)

        refreshTokenRepository.save(
            RefreshToken(
                0,
                refreshTokenData.second,
                jwtService.extractExpiration(refreshTokenData.first)!!.toInstant(),
                false,
                user
            )
        )
        return TokenPair(accessTokenData.first, refreshTokenData.first)
    }
}