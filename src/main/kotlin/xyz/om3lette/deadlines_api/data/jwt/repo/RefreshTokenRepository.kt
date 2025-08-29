package xyz.om3lette.deadlines_api.data.jwt.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import xyz.om3lette.deadlines_api.data.jwt.model.RefreshToken
import xyz.om3lette.deadlines_api.data.user.model.User
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByJti(jti: String): Optional<RefreshToken>

    @Query(
        "SELECT r FROM RefreshToken r WHERE r.expiry > CURRENT_TIMESTAMP AND r.user = :user AND r.revoked = false"
    )
    fun findAllValidByUser(user: User): List<RefreshToken>
}