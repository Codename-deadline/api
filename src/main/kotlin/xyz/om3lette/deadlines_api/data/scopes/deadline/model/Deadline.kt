package xyz.om3lette.deadlines_api.data.scopes.deadline.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import xyz.om3lette.deadlines_api.data.notifications.model.DeadlineNotification
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeTextConstraints
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlineDTO
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlinePermissions
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlineStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.deadline.response.DeadlineResponse
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import java.time.Instant

@Entity
@Table(name = "deadlines")
data class Deadline(
    @Id
    @SequenceGenerator(name = "deadline_seq", sequenceName = "deadline_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deadline_seq")
    val id: Long = 0,

    // Thread id is needed for global role lookup
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "thread_id", nullable = false)
    val thread: Thread,

    @field:NotBlank
    @field:Size(min = ScopeTextConstraints.TITLE_MIN, max = ScopeTextConstraints.TITLE_MAX)
    @Column(length = ScopeTextConstraints.TITLE_MAX, nullable = false)
    var title: String,

    @field:Size(max = ScopeTextConstraints.DESCRIPTION_MAX)
    @Column(length = ScopeTextConstraints.DESCRIPTION_MAX)
    var description: String?,

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val createdAt: Instant,

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    var due: Instant,

    @Column(nullable = false)
    var isCompleted: Boolean = false,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "deadline")
    @OnDelete(action = OnDeleteAction.CASCADE)
    val notifications: MutableList<DeadlineNotification> = mutableListOf(),

) {
    fun toMap() = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "createdAt" to createdAt,
        "due" to due,
        "isCompleted" to isCompleted
    )

    fun toDTO() = DeadlineDTO(
        id, title, description, createdAt, due, isCompleted, thread.id
    )

    fun toResponse(stats: DeadlineStatsDTO, permissions: DeadlinePermissions) = DeadlineResponse(
        deadline = toDTO(),
        stats = stats.toResponse(),
        permissions = permissions
    )
}
