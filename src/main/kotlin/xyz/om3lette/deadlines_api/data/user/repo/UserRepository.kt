package xyz.om3lette.deadlines_api.data.user.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import xyz.om3lette.deadlines_api.data.user.model.User
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE LOWER(u._username) IN :usernames")
    fun findByUsernameInIgnoreCase(@Param("usernames") usernamesLower: List<String>): List<User>

    @Query("SELECT u FROM User u WHERE LOWER(u._username) = LOWER(:username)")
    fun findByUsernameIgnoreCase(username: String): Optional<User>

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u._username) = LOWER(:username)")
    fun existsByUsernameIgnoreCase(username: String): Boolean
}