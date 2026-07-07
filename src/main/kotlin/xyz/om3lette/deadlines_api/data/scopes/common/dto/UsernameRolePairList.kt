package xyz.om3lette.deadlines_api.data.scopes.common.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.scopes.common.constraints.ScopeInvitationConstraints
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType

data class UsernameRolePairList @JsonCreator constructor(
    @get:JsonValue
    @field:Valid
    @field:Size(max = ScopeInvitationConstraints.MAX_INVITATIONS)
    val usernameRolePairs: List<@Valid UsernameRolePair> = emptyList(),
) {
    fun filterByScope(scopeType: ScopeType): List<UsernameRolePair> =
        usernameRolePairs.filter { it.role.canBeAssignedInScope(scopeType) }
}
