package xyz.om3lette.deadlines_api.data.scopes.userScope.repo

import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.user.model.User
import java.util.Optional

interface UserScopeRepository : JpaRepository<UserScope, Long> {

    fun findByUserAndScopeId(
        user: User,
        scopeId: Long
    ): Optional<UserScope>

    fun findByUserAndScopeIdIn(
        user: User,
        scopeId: List<Long>
    ): List<UserScope>

    @Query("""
        SELECT us FROM UserScope us
        WHERE us.scopeId = :scopeId AND LOWER(us.user._username) = :username
    """)
    fun findByUsernameAndScopeIdIgnoreCase(username: String, scopeId: Long): Optional<UserScope>

    @Query("""
        SELECT us FROM UserScope us
        WHERE us.scopeId in :scopeIds AND LOWER(us.user._username) in :usernames
    """)
    fun findByScopeIdInAndUsernameInIgnoreCase(
        scopeIds: Long,
        @Param("usernames") usernamesLower: List<String>
    ): List<UserScope>

    @Query("""
        SELECT us FROM UserScope us
        WHERE us.scopeId = :scopeId
        AND LOWER(us.user._username) in :usernames
    """)
    fun findByScopeIdAndUsernameInIgnoreCase(
        scopeId: Long,
        @Param("usernames") usernamesLower: List<String>
    ): List<UserScope>

    fun findAllByScopeId(
        scopeId: Long,
        pageable: Pageable
    ): Page<UserScope>


    @Modifying
    @Transactional
    @Query(
        """
            DELETE FROM user_scopes us
            WHERE us.user_id = :userId
                AND (
                    (us.scope_type = 'ORG' AND us.scope_id = :orgId)
                    OR (us.scope_type = 'THR' AND us.scope_id = :thrId)
                    OR (us.scope_type = 'DDL' AND us.scope_id = :ddlId)
                )
        """,
        nativeQuery = true
    )
    fun deleteByUserAndScopeId(
        user: User,
        orgId: Long?,
        thrId: Long?,
        ddlId: Long?,
    ): Int

    @Modifying
    @Transactional
    fun deleteByUserAndScopeIdIn(
        user: User,
        scopeId: Collection<Long>
    ): Int
}