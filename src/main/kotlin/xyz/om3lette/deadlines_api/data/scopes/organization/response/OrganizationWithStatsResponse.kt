package xyz.om3lette.deadlines_api.data.scopes.organization.response

import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationStatsDTO
import java.time.Instant

data class OrganizationWithStatsResponse(
    val id: Long,

    val title: String,

    val description: String?,

    val type: OrganizationType,

    val createdAt: Instant,

    val stats: OrganizationStatsResponse
) {
    companion object {
        fun fromResponseAndStats(response: OrganizationResponse, statsDTO: OrganizationStatsDTO) =
            OrganizationWithStatsResponse(
                id = response.id,
                title = response.title,
                description = response.description,
                type = response.type,
                createdAt = response.createdAt,
                stats = statsDTO.toResponse()
            )
    }
}