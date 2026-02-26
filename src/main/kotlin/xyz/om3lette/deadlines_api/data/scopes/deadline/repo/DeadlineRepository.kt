package xyz.om3lette.deadlines_api.data.scopes.deadline.repo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread

interface DeadlineRepository : JpaRepository<Deadline, Long> {

    @Query("SELECT d.id FROM Deadline d WHERE d.organization.id = :orgId")
    fun findAllIdsByOrganizationId(@Param("orgId") organizationId: Long): List<Long>

//    @Query("SELECT d.id FROM Deadline d WHERE d.thread.id = :threadId")
    fun findAllByThread(thread: Thread, pageable: Pageable): Page<Deadline>
}