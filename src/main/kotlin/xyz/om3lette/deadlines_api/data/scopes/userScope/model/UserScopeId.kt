package xyz.om3lette.deadlines_api.data.scopes.userScope.model

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import java.io.Serializable

data class UserScopeId(
    val user: Long = 0,
    val scopeType: ScopeType = ScopeType.ORGANIZATION,
    val scopeId: Long = 0
) : Serializable
