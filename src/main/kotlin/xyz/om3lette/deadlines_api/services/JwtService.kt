package xyz.om3lette.deadlines_api.services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @param:Value("\${spring.security.jwt.secret}") private val jwtSecret: String
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

    @Value("\${spring.security.jwt.access.expiration}")
    private val jwtAccessExpiration: Long = 5 * 60

    @Value("\${spring.security.jwt.refresh.expiration}")
    private val jwtRefreshExpiration: Long = 7 * 24 * 60 * 60

    private fun generateToken(
        userDetails: UserDetails,
        expiration: Long,
        extraClaims: Map<String?, Any?>,
    ): Pair<String, String> {
        val jti: String = UUID.randomUUID().toString()
        val mutableClaims = HashMap(extraClaims)
        mutableClaims["jti"] = jti

        return Pair(buildToken(userDetails, expiration, mutableClaims), jti)
    }

    private fun buildToken(
        userDetails: UserDetails,
        expiration: Long,
        extraClaims: Map<String?, Any?>
    ): String {
        return Jwts
            .builder()
            .subject(userDetails.username)
            .claims(extraClaims)
            .issuedAt(Date(Instant.now().toEpochMilli()))
            .expiration(Date(Instant.now().toEpochMilli() + expiration * 1000))
            .signWith(secretKey)
            .compact()
    }

    fun generateAccessToken(userDetails: UserDetails): Pair<String, String> {
        return generateToken(userDetails, jwtAccessExpiration, HashMap())
    }

    fun generateRefreshToken(userDetails: UserDetails): Pair<String, String> {
        return generateToken(userDetails, jwtRefreshExpiration, mapOf("refresh" to true))
    }

    fun extractAllClaims(token: String): Claims {
        return Jwts
            .parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun <T> extractClaim(token: String, claimRetriever: (Claims) -> T): T? {
        val claims: Claims = extractAllClaims(token)
        return claimRetriever(claims)
    }

    fun extractUsername(token: String): String? {
        return extractClaim<String>(token, Claims::getSubject)
    }

    fun extractExpiration(token: String): Date? {
        return extractClaim<Date>(token, Claims::getExpiration)
    }

    fun isTokenValid(token: String, userId: String): Boolean {
//        extractAllClaims will validate the expiry by default
        return extractUsername(token) == userId
    }
}
