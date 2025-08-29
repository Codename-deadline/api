package xyz.om3lette.deadlines_api.data.scopes.organization.repo

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.user.model.User

interface OrganizationRepository : JpaRepository<Organization, Long> {
    @Query("""
        SELECT o FROM Organization o
        JOIN UserScope us ON us.scopeId = o.id AND us.scopeType = 'O'
        WHERE us.user = :user
    """)
    fun findAllOrganizationsForUser(@Param("user") user: User, pageable: Pageable): List<Organization>

}
