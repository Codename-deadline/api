package xyz.om3lette.deadlines_api.data.scopes.thread.repo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
interface ThreadRepository : JpaRepository<Thread, Long> {

    @Query("SELECT t.id FROM Thread t WHERE t.organization.id = :orgId")
    fun findAllIdsByOrganizationId(@Param("orgId") organizationId: Long): List<Long>

    fun findAllByOrganization(organization: Organization, pageable: Pageable): Page<Thread>
}