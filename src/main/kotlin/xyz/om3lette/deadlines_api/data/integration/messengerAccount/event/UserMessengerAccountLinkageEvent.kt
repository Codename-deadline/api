package xyz.om3lette.deadlines_api.data.integration.messengerAccount.event

data class UserMessengerAccountLinkageEvent(
    val requestId: String,

    val accountId: Long
)
