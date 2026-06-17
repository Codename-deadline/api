package xyz.om3lette.deadlines_api.data.scopes.deadline.repo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlineStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.thread.dto.ThreadStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread

interface DeadlineRepository : JpaRepository<Deadline, Long> {

    @Query("SELECT d.id FROM Deadline d WHERE d.organization.id = :orgId")
    fun findAllIdsByOrganizationId(@Param("orgId") organizationId: Long): List<Long>

    fun findAllByThread(thread: Thread, pageable: Pageable): Page<Deadline>

    @Query("""
        SELECT
            d.id as deadlineId,
            COUNT(us.id) as assignees,
            COUNT(at.id) as attachments
        FROM Deadline d
        LEFT JOIN UserScope us ON us.scopeId = d.id AND us.scopeType = 'DDL'
        LEFT JOIN Attachment at ON at.deadline = d
        WHERE d.id IN :ids
        GROUP BY d.id
    """)
    fun getDeadlineStats(@Param("ids") deadlineIds: List<Long>): List<DeadlineStatsDTO>

    @Query("""
        SELECT d
        FROM Deadline d  
        WHERE EXISTS (  
            SELECT 1  
            FROM UserScope us  
            WHERE us.scopeId = d.id  
              AND us.scopeType = 'DDL'  
              AND us.user.id = :userId  
        )
    """)
    fun findAllByUser(userId: Long, pageable: Pageable): Page<Deadline>
}