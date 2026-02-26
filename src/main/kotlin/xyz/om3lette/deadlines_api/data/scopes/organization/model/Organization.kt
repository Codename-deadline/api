package xyz.om3lette.deadlines_api.data.scopes.organization.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import org.hibernate.annotations.SQLRestriction
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationResponse
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import java.time.Instant

@Entity
@Table(name = "organizations")
data class Organization(
    @Id
    @SequenceGenerator(name = "org_seq", sequenceName = "org_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "org_seq")
    val id: Long = 0,

    @field:Size(min = 2, max = 128)
    var title: String,

    @field:Size(max = 2048)
    var description: String?,

    @Enumerated(value = EnumType.STRING)
    val type: OrganizationType,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val createdAt: Instant,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(
        name = "scope_id",
        referencedColumnName = "id",
        insertable = false, updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    @SQLRestriction("scope_type = 'O'")
    val members: MutableList<UserScope> = mutableListOf(),

    @OneToMany(mappedBy = "organization", cascade = [CascadeType.ALL], orphanRemoval = true)
    val threads: MutableList<Thread> = mutableListOf(),

    @OneToMany(mappedBy = "organization", cascade = [CascadeType.ALL], orphanRemoval = true)
    val invitations: MutableList<OrganizationInvitation> = mutableListOf()
) {
    fun toMap() = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "type" to type,
        "createdAt" to createdAt
    )

    fun toResponse() = OrganizationDTO(
        id, title, description, type, createdAt
    )

    fun toStatsResponse(stats: OrganizationStatsDTO) = OrganizationResponse(
        organization = toResponse(),
        stats = stats.toResponse()
    )
}
