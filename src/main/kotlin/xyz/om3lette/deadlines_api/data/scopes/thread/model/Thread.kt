package xyz.om3lette.deadlines_api.data.scopes.thread.model

import jakarta.persistence.*
import jakarta.validation.constraints.Size
import org.hibernate.annotations.SQLRestriction
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.thread.response.ThreadResponse
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import java.time.Instant

@Entity
@Table(name = "threads")
data class Thread(
    @Id
    @SequenceGenerator(name = "thread_seq", sequenceName = "thread_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "thread_seq")
    val id: Long = 0,

    @field:Size(min = 2, max = 128)
    var title: String,

    @field:Size(max = 2048)
    var description: String?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    val organization: Organization,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val createdAt: Instant,

    @OneToMany(mappedBy = "thread", cascade = [CascadeType.ALL], orphanRemoval = true)
    val deadlines: MutableList<Deadline> = mutableListOf(),

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(
        name = "scope_id",
        referencedColumnName = "id",
        insertable = false, updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    @SQLRestriction("scope_type = 'THR'")
    val assignees: MutableList<UserScope> = mutableListOf()
) {
    fun toMap() = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "organizationId" to organization.id
    )

    fun toResponse() = ThreadResponse(
        id, title, description, organization.id
    )
}
