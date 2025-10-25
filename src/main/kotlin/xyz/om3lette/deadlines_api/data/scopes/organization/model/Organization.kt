package xyz.om3lette.deadlines_api.data.scopes.organization.model

import jakarta.persistence.*
import jakarta.validation.constraints.Size
import org.hibernate.annotations.SQLRestriction

import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationResponse
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.user.model.User
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

    @Enumerated(value = EnumType.ORDINAL)
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

    fun toResponse() = OrganizationResponse(
        id, title, description, type, createdAt
    )
}
