package xyz.om3lette.deadlines_api.data.scopes.common.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType

data class UsernameRolePairList @JsonCreator constructor(
    @get:JsonValue
    val usernameRolePairs: List<UsernameRolePair> = emptyList(),
) {
    fun filterByScope(scopeType: ScopeType): List<UsernameRolePair> =
        usernameRolePairs.filter { it.role.canBeAssignedInScope(scopeType) }
}
