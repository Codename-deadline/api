package xyz.om3lette.deadlines_api.data.scopes.userScope.model

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.scopes.userScope.dto.ScopeRoleDTO
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.response.UserScopeResponse
import java.time.Instant

@Entity
@Table(name = "user_scopes")
@IdClass(UserScopeId::class)
data class UserScope(
    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", length = 12, nullable = false)
    val scopeType: ScopeType,

    @Id
    @Column(name = "scope_id", nullable = false)
    val scopeId: Long,

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    var role: ScopeRole,

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val assignedAt: Instant
) {
    fun toMap() = mapOf(
        "user" to user.toMap(),
        "role" to role,
        "assignedAt" to assignedAt
    )

    fun toDTO() = ScopeRoleDTO(
        role, scopeId, scopeType
    )

    fun toResponse() = UserScopeResponse(
        user.toResponse(), role, assignedAt
    )
}
