package xyz.om3lette.deadlines_api.util.userMessengerRepository

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException

fun UserMessengerAccountRepository.findByMessengerAndAccountIdOr404(
    messenger: Messenger,
    accountId: Long
) =
    findByMessengerAndAccountId(messenger, accountId).orElseThrow {
        StatusCodeException(404, ErrorCode.INTEGRATION_ACCOUNT_NOT_LINKED)
    }