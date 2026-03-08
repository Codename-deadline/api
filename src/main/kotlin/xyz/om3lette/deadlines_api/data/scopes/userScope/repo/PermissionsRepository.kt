package xyz.om3lette.deadlines_api.data.scopes.userScope.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import java.util.Optional

interface PermissionsRepository : JpaRepository<UserScope, Long> {
    @Query(
        """
        SELECT role FROM user_scopes us
        WHERE us.user_id = :userId
            AND (
                (us.scope_type = 'ORG' AND us.scope_id = :orgId)
                OR (us.scope_type = 'THR' AND us.scope_id = :thrId)
                OR (us.scope_type = 'DDL' AND us.scope_id = :ddlId)
            )
        ORDER BY role DESC
        LIMIT 1
        """,
        nativeQuery = true
    )
    fun findHighestRoleByUser(userId: Long, orgId: Long?, thrId: Long?, ddlId: Long?): ScopeRole?
}