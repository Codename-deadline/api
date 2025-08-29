package xyz.om3lette.deadlines_api.data.scopes.organization.repo

import org.springframework.data.jpa.repository.JpaRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationInvitation

interface OrganizationInvitationRepository : JpaRepository<OrganizationInvitation, Long> {
}