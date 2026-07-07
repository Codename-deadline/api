package xyz.om3lette.deadlines_api.data.scopes.userScope.repo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.permissions.dto.PermissionRoleDTO
import xyz.om3lette.deadlines_api.data.scopes.userScope.dto.ScopeRoleDTO
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.user.model.User
import java.util.Optional

interface UserScopeRepository : JpaRepository<UserScope, Long> {
    @Query("""
        SELECT
            us.user.id as userId,
            us.role as role
        FROM UserScope us
        WHERE us.scopeId = :scopeId
            AND us.scopeType = :scopeType
            AND LOWER(us.user._username) = :usernameLower
    """)
    fun findRoleAndUserIdByUsernameLowerAndScopeIdAndScopeType(
        usernameLower: String, scopeId: Long, scopeType: ScopeType
    ): PermissionRoleDTO?

    fun findByUserAndScopeIdAndScopeType(
        user: User,
        scopeId: Long,
        scopeType: ScopeType
    ): Optional<UserScope>

    fun existsByUserAndScopeIdAndScopeType(
        user: User,
        scopeId: Long,
        scopeType: ScopeType
    ): Boolean

    @Query("""
        SELECT us FROM UserScope us
        WHERE us.scopeId = :scopeId
            AND :scopeType = us.scopeType
            AND LOWER(us.user._username) = :username
    """)
    fun findByScopeTypeAndScopeIdAndUsernameIgnoreCase(username: String, scopeType: ScopeType, scopeId: Long): Optional<UserScope>

    @Query("""
        SELECT us FROM UserScope us
        WHERE us.scopeType = :scopeType
            AND us.scopeId IN :scopeIds
            AND LOWER(us.user._username) IN :usernames
    """)
    fun findByScopeTypeScopeIdInAndUsernameInIgnoreCase(
        scopeIds: Long,
        scopeType: ScopeType,
        @Param("usernames") usernamesLower: List<String>
    ): List<UserScope>

    @Query("""
        SELECT us FROM UserScope us
        WHERE us.scopeId = :scopeId
            AND us.scopeType = :scopeType
            AND LOWER(us.user._username) IN :usernames
    """)
    fun findByScopeIdAndScopeTypeAndUsernameInIgnoreCase(
        scopeId: Long,
        scopeType: ScopeType,
        @Param("usernames") usernamesLower: List<String>
    ): List<UserScope>

    fun findAllByScopeIdAndScopeType(
        scopeId: Long,
        scopeType: ScopeType,
        pageable: Pageable
    ): Page<UserScope>

    @Query("""
        SELECT COUNT(us) FROM UserScope us
        WHERE us.scopeId = :deadlineId
            AND us.scopeType = :scopeType
    """)
    fun countDeadlineAssignees(deadlineId: Long, scopeType: ScopeType = ScopeType.DEADLINE): Long

    @Query("""
        SELECT us FROM UserScope us
        WHERE us.scopeId = :deadlineId
            AND us.scopeType = :scopeType
        ORDER BY us.role DESC, us.assignedAt ASC
    """)
    fun findAllDeadlineAssignees(deadlineId: Long, scopeType: ScopeType = ScopeType.DEADLINE): List<UserScope>


    @Modifying
    @Transactional
    @Query(
        """
            DELETE FROM UserScope us
            WHERE us.user.id = :userId
                AND (
                    (us.scopeType = 'ORG' AND us.scopeId = :orgId)
                    OR (us.scopeType = 'THR' AND us.scopeId = :thrId)
                    OR (us.scopeType = 'DDL' AND us.scopeId = :ddlId)
                )
        """
    )
    fun deleteByUserIdAndScopeId(
        userId: Long,
        orgId: Long?,
        thrId: Long?,
        ddlId: Long?,
    ): Int

    @Modifying
    @Transactional
    fun deleteByUserIdAndScopeTypeAndScopeIdIn(
        userId: Long,
        scopeType: ScopeType,
        scopeId: Collection<Long>
    ): Int

    @Query(
        """
        SELECT role, scopeId, scopeType FROM UserScope us
        WHERE us.user.id = :userId
            AND (
                (us.scopeType = 'ORG' AND us.scopeId = :orgId)
                OR (us.scopeType = 'THR' AND us.scopeId = :thrId)
                OR (us.scopeType = 'DDL' AND us.scopeId = :ddlId)
            )
        """
    )
    fun findUserRolesInScope(userId: Long, orgId: Long?, thrId: Long?, ddlId: Long?): List<ScopeRoleDTO>

    @Query(
        """
        SELECT role, scopeId, scopeType FROM UserScope us
        WHERE us.user.id = :userId
            AND (
                (us.scopeType = 'ORG' AND us.scopeId IN :orgIds)
                OR (us.scopeType = 'THR' AND us.scopeId IN :thrIds)
                OR (us.scopeType = 'DDL' AND us.scopeId IN :ddlIds)
            )
        """
    )
    fun findUserRolesInScopes(userId: Long, orgIds: List<Long>, thrIds: List<Long>, ddlIds: List<Long>): List<ScopeRoleDTO>
}
