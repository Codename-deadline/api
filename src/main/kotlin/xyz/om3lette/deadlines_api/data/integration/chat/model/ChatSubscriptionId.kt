package xyz.om3lette.deadlines_api.data.integration.chat.model

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import java.io.Serializable

data class ChatSubscriptionId(
    val chat: Long = 0,
    val scopeId: Long = 0,
    val scopeType: ScopeType = ScopeType.ORGANIZATION
) : Serializable
