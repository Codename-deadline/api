package xyz.om3lette.deadlines_api.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.common.response.PaginationResponse
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationResponse
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.util.page.toPaginationResponse

@Service
class UserService(
    private val organizationRepository: OrganizationRepository
) {
    // TODO: Separate personal org from the rest
    fun getOrganizationsByUser(user: User, pageNumber: Int, pageSize: Int): PaginationResponse<OrganizationResponse> {
        val organizations: Page<Organization> = organizationRepository.findAllOrganizationsForUser(
            user, PageRequest.of(pageNumber, pageSize)
        )
        val stats = organizationRepository.getOrganizationsStats(
            organizations.content.map { it.id }
        ).associateBy { it.organizationId }

        return organizations.toPaginationResponse {
            it.toStatsResponse(stats[it.id]!!)
        }
    }
}
