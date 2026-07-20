package xyz.om3lette.deadlines_api.data.scopes.organization.repo

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.user.model.User
import java.util.Optional

interface OrganizationRepository : JpaRepository<Organization, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Organization o WHERE o.id = :organizationId")
    fun findByIdForUpdate(@Param("organizationId") organizationId: Long): Organization?

    @Query("""
        SELECT o FROM Organization o
        JOIN UserScope us ON us.scopeId = o.id AND us.scopeType = 'ORG'
        WHERE us.user = :user
        ORDER BY CASE WHEN o.type = 'PERSONAL' THEN 0 ELSE 1 END
    """)
    fun findAllOrganizationsForUser(@Param("user") user: User, pageable: Pageable): Page<Organization>

    @Query("""
        SELECT
            o.id as organizationId,
            (SELECT COUNT(us) FROM UserScope us WHERE us.scopeId = o.id AND us.scopeType = 'ORG') as members,
            (SELECT COUNT(t) FROM Thread t WHERE t.organization = o) as threads
        FROM Organization o
        WHERE o.id IN :ids
    """)
    fun getOrganizationsStats(@Param("ids") organizationIds: List<Long>): List<OrganizationStatsDTO>
}
