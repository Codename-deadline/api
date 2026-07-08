package xyz.om3lette.deadlines_api.services.auth

import io.jsonwebtoken.Claims
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.configs.properties.UsersProperties
import xyz.om3lette.deadlines_api.data.jwt.dto.TokenPair
import xyz.om3lette.deadlines_api.data.jwt.model.RefreshToken
import xyz.om3lette.deadlines_api.data.jwt.repo.RefreshTokenRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.JwtService

@Service
class AuthSessionService(
    private val usersProperties: UsersProperties,
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun issueSession(user: User): TokenPair {
        val openedSessions = refreshTokenRepository.findAllValidByUser(user).count()

        if (openedSessions >= usersProperties.maxSessions) {
            throw StatusCodeException(
                statusCode = 400,
                code = ErrorCode.AUTH_SESSIONS_LIMIT_EXCEEDED,
                detail = "Sessions limit reached: $openedSessions",
                params = mapOf(
                    "opened" to openedSessions,
                    "max" to usersProperties.maxSessions
                )
            )
        }

        return generateTokenPair(user)
    }

    fun refreshSession(refreshTokenJwt: String): TokenPair {
        val claims: Claims = try {
            jwtService.extractAllClaims(refreshTokenJwt)
        } catch (_: Exception) {
            throw StatusCodeException(401, ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val username = claims.subject
        val jti = claims["jti"] as String?

        if (username == null || jti == null) {
            throw StatusCodeException(401, ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow { StatusCodeException(401, ErrorCode.AUTH_INVALID_CREDENTIALS) }
        val refreshTokenEntry = refreshTokenRepository.findByJti(jti)
            .orElseThrow { StatusCodeException(401, ErrorCode.AUTH_INVALID_CREDENTIALS) }

        if (refreshTokenEntry.revoked) {
            throw StatusCodeException(401, ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        refreshTokenEntry.revoked = true
        refreshTokenRepository.save(refreshTokenEntry)

        return generateTokenPair(user)
    }

    fun revokeAllSessions(user: User) {
        val userValidTokens = refreshTokenRepository.findAllValidByUser(user)

        userValidTokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(userValidTokens)
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
