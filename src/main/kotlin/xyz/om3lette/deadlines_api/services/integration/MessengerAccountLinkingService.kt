package xyz.om3lette.deadlines_api.services.integration

import io.grpc.Status
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.data.integration.common.response.IntegrationResult
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.event.UserMessengerAccountLinkageEvent
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.model.AccountLinkageRequest
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.repo.AccountLinkageRepository
import xyz.om3lette.deadlines_api.services.integration.kafka.AccountLinkageProducer
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.requirePermission
import java.util.UUID

@Service
class MessengerAccountLinkingService(
    private val userMessengerAccountRepository: UserMessengerAccountRepository,
    private val accountLinkageProducer: AccountLinkageProducer,
    private val accountLinkageRepository: AccountLinkageRepository,
    private val permissionService: PermissionService,
    private val userRepository: UserRepository,
    private val languageResolver: IntegrationLanguageResolver,
) {
    private val logger = LoggerFactory.getLogger(MessengerAccountLinkingService::class.java)

    fun sendConfirmationForAccountLinkage(
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

        val requestId = UUID.randomUUID().toString()
        accountLinkageRepository.save(
            AccountLinkageRequest(requestId, accountId, messenger, user.id)
        )
        accountLinkageProducer.sendToMessenger(messenger, UserMessengerAccountLinkageEvent(requestId, accountId))
    }

    fun linkMessengerAccount(
        requestId: String,
        isAccepted: Boolean,
        messengerAccountId: Long,
        messenger: Messenger,
    ): IntegrationResult {
        val linkAccountRequest = accountLinkageRepository.findById(requestId).orElseThrow {
            grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.REQUEST_NOT_FOUND,
                languageResolver.fallbackLanguage
            )
        }

        if (
            linkAccountRequest.accountId != messengerAccountId ||
            linkAccountRequest.messenger != messenger
        ) {
            throw grpcException(
                Status.PERMISSION_DENIED,
                IntegrationResultKey.REQUEST_NOT_FOUND,
                languageResolver.fallbackLanguage
            )
        }

        if (!isAccepted) {
            accountLinkageRepository.delete(linkAccountRequest)
            logger.info("Account linkage request $requestId declined")
            val language = userRepository.findById(linkAccountRequest.userId)
                .map { it.language }
                .orElse(languageResolver.fallbackLanguage)
            return integrationResult(IntegrationResultKey.ACCOUNT_LINKAGE_IGNORED, language)
        }

        val user = userRepository.findById(linkAccountRequest.userId).orElseThrow {
            grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.USER_NOT_FOUND,
                languageResolver.fallbackLanguage
            )
        }
        userMessengerAccountRepository.save(
            UserMessengerAccount(
                0,
                user,
                linkAccountRequest.accountId,
                linkAccountRequest.messenger
            )
        )
        accountLinkageRepository.delete(linkAccountRequest)
        logger.info("Account linkage request $requestId accepted")

        return integrationResult(IntegrationResultKey.ACCOUNT_LINKAGE_SUCCESS, user.language)
    }
}
