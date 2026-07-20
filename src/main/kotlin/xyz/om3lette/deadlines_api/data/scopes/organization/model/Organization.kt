package xyz.om3lette.deadlines_api.data.scopes.organization.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeTextConstraints
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationPermissions
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationResponse
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import java.time.Instant

@Entity
@Table(name = "organizations")
data class Organization(
    @Id
    @SequenceGenerator(name = "org_seq", sequenceName = "org_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "org_seq")
    val id: Long = 0,

    @field:NotBlank
    @field:Size(min = ScopeTextConstraints.TITLE_MIN, max = ScopeTextConstraints.TITLE_MAX)
    @Column(length = ScopeTextConstraints.TITLE_MAX, nullable = false)
    var title: String,

    @field:Size(max = ScopeTextConstraints.DESCRIPTION_MAX)
    @Column(length = ScopeTextConstraints.DESCRIPTION_MAX)
    var description: String?,

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    var type: OrganizationType,

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val createdAt: Instant,

    @OneToMany(mappedBy = "organization", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val threads: MutableList<Thread> = mutableListOf(),

    @OneToMany(mappedBy = "organization", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val invitations: MutableList<OrganizationInvitation> = mutableListOf()
) {
    fun toMap() = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "type" to type,
        "createdAt" to createdAt
    )

    fun toDTO() = OrganizationDTO(
        id, title, description, type, createdAt
    )

    fun toResponse(stats: OrganizationStatsDTO, permissions: OrganizationPermissions) = OrganizationResponse(
        organization = toDTO(),
        stats = stats.toResponse(),
        permissions = permissions
    )
}
