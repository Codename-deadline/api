package xyz.om3lette.deadlines_api.services.integration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.event.UserMessengerAccountLinkageEvent
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.model.AccountLinkageRequest
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.repo.AccountLinkageRepository
import xyz.om3lette.deadlines_api.services.integration.kafka.AccountLinkageProducer
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.MessageResponse
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import xyz.om3lette.deadlines_api.util.requirePermission
import java.util.UUID

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
    ): MessageResponse {
        userMessengerAccountRepository.findByMessengerAndAccountId(messenger, accountId).ifPresent {
            throw StatusCodeException(403, "Account already in use")
        }

        val linkedMessengerAccounts = userMessengerAccountRepository.findAllByUserAndMessenger(user, messenger)
        requirePermission(
            permissionService.canLinkAccount(user, linkedMessengerAccounts.size),
            { "Account linkage limit exceeded for messenger" }
        )

        val requestId: String = UUID.randomUUID().toString()
        accountLinkageRepository.save(
            AccountLinkageRequest(requestId, accountId, messenger, user.id)
        )
        accountLinkageProducer.sendToMessenger(messenger, UserMessengerAccountLinkageEvent(requestId, accountId))
        return MessageResponse.success("Message will be sent shortly")
    }
}