package xyz.om3lette.deadlines_api.data.scopes.deadline.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import org.hibernate.annotations.SQLRestriction
import xyz.om3lette.deadlines_api.data.notifications.model.DeadlineNotification
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlineDTO
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlinePermissions
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlineStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.deadline.response.DeadlineResponse
import xyz.om3lette.deadlines_api.data.scopes.enums.ProgressionStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import java.time.Instant

@Entity
@Table(name = "deadlines")
data class Deadline(
    @Id
    @SequenceGenerator(name = "deadline_seq", sequenceName = "deadline_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deadline_seq")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER) // Used for deadline access permission check
    @JoinColumn(name = "organization_id")
    val organization: Organization,

    // TODO: This likely fetches the organization in a chain. Verify that and remove organization from deadline if true
    @ManyToOne(fetch = FetchType.EAGER) // Thread id is needed for global role lookup
    @JoinColumn(name = "thread_id")
    val thread: Thread,

    @field:Size(min = 2, max = 128)
    var title: String,

    @field:Size(max = 2048)
    var description: String?,

    @Enumerated(value = EnumType.STRING)
    var status: ProgressionStatus,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val createdAt: Instant,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    var due: Instant,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "deadline")
    val notifications: MutableList<DeadlineNotification> = mutableListOf(),

    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(
        name = "scope_id",
        referencedColumnName = "id",
        insertable = false, updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    @SQLRestriction("scope_type = 'DDL'")
    val assignees: MutableList<UserScope> = mutableListOf(),
) {
    fun toMap() = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "createdAt" to createdAt,
        "due" to due,
        "status" to status
    )

    fun toDTO() = DeadlineDTO(
        id, title, description, createdAt, due, status, organization.id
    )

    fun toResponse(stats: DeadlineStatsDTO, permissions: DeadlinePermissions) = DeadlineResponse(
        deadline = toDTO(),
        stats = stats.toResponse(),
        permissions = permissions
    )
}
