package xyz.om3lette.deadlines_api.data.scopes.deadline.model

import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.hibernate.annotations.SQLRestriction
import xyz.om3lette.deadlines_api.data.notifications.model.DeadlineNotification
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

    @ManyToOne
    @JoinColumn(name = "thread_id")
    val thread: Thread,

    @field:Size(min = 2, max = 128)
    var title: String,

    @field:Size(max = 2048)
    var description: String?,

    @field:Min(value = 0, "Deadline progress cannot be lower than 0%")
    @field:Max(value = 100, "Deadline progress cannot exceed 100%")
    var progress: Short,

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
    @SQLRestriction("scope_type = 'D'")
    val assignees: MutableList<UserScope> = mutableListOf(),
) {
    fun toMap() = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "createdAt" to createdAt,
        "due" to due,
        "progress" to progress,
        "status" to status
    )
}
