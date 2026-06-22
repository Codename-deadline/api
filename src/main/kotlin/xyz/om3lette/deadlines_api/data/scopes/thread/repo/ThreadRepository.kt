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
        SELECT t
        FROM Thread t 
        WHERE EXISTS (  
            SELECT 1  
            FROM UserScope us  
            WHERE us.scopeId = t.id  
              AND us.scopeType = 'THR'  
              AND us.user.id = :userId  
        )
    """)
    fun findAllByUser(userId: Long, pageable: Pageable): Page<Thread>

    @Query("""
        SELECT
            t.id as threadId,
            (SELECT COUNT(us.id) FROM UserScope us WHERE us.scopeId = t.id AND us.scopeType = 'THR') as assignees,
            (SELECT COUNT(d.id) FROM Deadline d WHERE d.thread = t) as deadlines,
            (SELECT COUNT(d.id) FROM Deadline d WHERE d.thread = t AND d.isCompleted = TRUE) as completedDeadlines
        FROM Thread t
        WHERE t.id IN :ids
    """)
    fun getThreadStats(@Param("ids") threads: List<Long>): List<ThreadStatsDTO>
}