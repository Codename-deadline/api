package xyz.om3lette.deadlines_api.data.scopes.thread.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeTextConstraints
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.thread.dto.ThreadDTO
import xyz.om3lette.deadlines_api.data.scopes.thread.dto.ThreadPermissions
import xyz.om3lette.deadlines_api.data.scopes.thread.dto.ThreadStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.thread.response.ThreadResponse
import java.time.Instant

@Entity
@Table(name = "threads")
data class Thread(
    @Id
    @SequenceGenerator(name = "thread_seq", sequenceName = "thread_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "thread_seq")
    val id: Long = 0,

    @field:NotBlank
    @field:Size(min = ScopeTextConstraints.TITLE_MIN, max = ScopeTextConstraints.TITLE_MAX)
    @Column(length = ScopeTextConstraints.TITLE_MAX, nullable = false)
    var title: String,

    @field:Size(max = ScopeTextConstraints.DESCRIPTION_MAX)
    @Column(length = ScopeTextConstraints.DESCRIPTION_MAX)
    var description: String?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val createdAt: Instant,

    @OneToMany(mappedBy = "thread", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val deadlines: MutableList<Deadline> = mutableListOf(),

) {
    fun toMap() = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "organizationId" to organization.id
    )

    fun toDTO() = ThreadDTO(
        id, title, description, organization.id, createdAt
    )

    fun toResponse(stats: ThreadStatsDTO, permissions: ThreadPermissions) = ThreadResponse(
        thread = toDTO(),
        stats = stats.toResponse(),
        permissions = permissions
    )
}
