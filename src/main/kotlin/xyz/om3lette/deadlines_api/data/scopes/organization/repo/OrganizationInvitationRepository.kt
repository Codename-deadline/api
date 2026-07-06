package xyz.om3lette.deadlines_api.data.scopes.organization.repo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationInvitation

interface OrganizationInvitationRepository : JpaRepository<OrganizationInvitation, Long> {
    @Query("""
        SELECT i
        FROM OrganizationInvitation i
        WHERE i.invitedUser.id = :userId
            AND i.status = 'PENDING'
    """)
    fun findAllPendingByInvitedUserId(userId: Long, pageable: Pageable): Page<OrganizationInvitation>

    @Query("""
        SELECT i
        FROM OrganizationInvitation i
        WHERE i.invitedBy.id = :userId
            AND i.status = 'PENDING'
    """)
    fun findAllPendingSentByUserId(userId: Long, pageable: Pageable): Page<OrganizationInvitation>

    @Query("""
        SELECT COUNT(i.id)
        FROM OrganizationInvitation i
        WHERE i.invitedUser.id = :userId
            AND i.status = 'PENDING'
    """)
    fun countPendingByInvitedUserId(userId: Long): Int
}
