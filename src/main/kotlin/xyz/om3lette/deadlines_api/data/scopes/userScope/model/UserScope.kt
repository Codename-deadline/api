package xyz.om3lette.deadlines_api.data.scopes.userScope.model

import jakarta.persistence.*

import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.scopes.userScope.converters.ScopeTypeConverter
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.response.UserScopeResponse
import java.time.Instant

@Entity
@Table(
    name = "user_scopes",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "scope_id", "scope_type"])
    ]
)
data class UserScope(
    @Id
    @SequenceGenerator(name = "scope_seq", sequenceName = "scope_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "scope_seq")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    val user: User,

    @Convert(converter = ScopeTypeConverter::class)
    val scopeType: ScopeType,

    @Column(name = "scope_id")
    val scopeId: Long,

    @Enumerated(value = EnumType.STRING)
    var role: ScopeRole,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val assignedAt: Instant
) {
    fun toMap() = mapOf(
        "user" to user.toMap(),
        "role" to role,
        "assignedAt" to assignedAt
    )

    fun toResponse() = UserScopeResponse(
        user.toResponse(), role, assignedAt
    )
}