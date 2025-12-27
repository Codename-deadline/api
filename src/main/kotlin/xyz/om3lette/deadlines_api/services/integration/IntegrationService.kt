package xyz.om3lette.deadlines_api.services.integration

import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.event.UserMessengerAccountLinkageEvent
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.model.AccountLinkageRequest
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.repo.AccountLinkageRepository
import xyz.om3lette.deadlines_api.services.integration.kafka.AccountLinkageProducer
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.requirePermission
import java.util.*

@Service
class IntegrationService(
    private val userMessengerAccountRepository: UserMessengerAccountRepository,
    private val accountLinkageProducer: AccountLinkageProducer,
    private val accountLinkageRepository: AccountLinkageRepository,
    private val permissionService: PermissionService
) {

    fun handleLinkMessengerAccountRequest(
        user: User,
        accountId: Long,
        messenger: Messenger
    ) {
        userMessengerAccountRepository.findByMessengerAndAccountId(messenger, accountId).ifPresent {
            throw StatusCodeException(403, ErrorCode.INTEGRATION_ACCOUNT_ALREADY_IN_USE)
        }

        val linkedMessengerAccounts = userMessengerAccountRepository.findAllByUserAndMessenger(user, messenger)
        requirePermission(
            permissionService.canLinkAccount(user, linkedMessengerAccounts.size),
            { ErrorCode.INTEGRATION_MESSENGER_LINKAGE_LIMIT_EXCEEDED to null },
            httpStatus = 409
        )

        val requestId: String = UUID.randomUUID().toString()
        accountLinkageRepository.save(
            AccountLinkageRequest(requestId, accountId, messenger, user.id)
        )
        accountLinkageProducer.sendToMessenger(messenger, UserMessengerAccountLinkageEvent(requestId, accountId))
    }
}