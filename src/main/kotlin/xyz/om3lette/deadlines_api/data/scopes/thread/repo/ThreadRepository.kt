package xyz.om3lette.deadlines_api.data.scopes.thread.repo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.thread.dto.ThreadStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
interface ThreadRepository : JpaRepository<Thread, Long> {

    @Query("SELECT t.id FROM Thread t WHERE t.organization.id = :orgId")
    fun findAllIdsByOrganizationId(@Param("orgId") organizationId: Long): List<Long>

    fun findAllByOrganization(organization: Organization, pageable: Pageable): Page<Thread>

    @Query("""
        SELECT
            t.id as threadId,
            COUNT(us.id) as assignees,
            COUNT(d.id) as deadlines,
            COUNT(CASE WHEN d.status = 'FINISHED' THEN 1 END) as completedDeadlines
        FROM Thread t
        LEFT JOIN UserScope us ON us.scopeId = t.id AND us.scopeType = 'THR'
        LEFT JOIN Deadline d ON d.thread = t
        WHERE t.id IN :ids
        GROUP BY t.id
    """)
    fun getThreadStats(@Param("ids") threads: List<Long>): List<ThreadStatsDTO>
}