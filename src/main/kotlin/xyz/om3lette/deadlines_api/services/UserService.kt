package xyz.om3lette.deadlines_api.services

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationResponse
import xyz.om3lette.deadlines_api.data.user.model.User

@Service
class UserService(
    private val organizationRepository: OrganizationRepository
) {
    // TODO: Separate personal org from the rest
    fun getOrganizationsByUser(user: User, pageNumber: Int, pageSize: Int): List<OrganizationResponse> =
        organizationRepository.findAllOrganizationsForUser(
            user, PageRequest.of(pageNumber, pageSize)
        ).map { it.toResponse() }
}
