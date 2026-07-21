package xyz.om3lette.deadlines_api.services.integration

import io.grpc.Status
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.chat.model.ChatSubscription
import xyz.om3lette.deadlines_api.data.integration.chat.repo.ChatRepository
import xyz.om3lette.deadlines_api.data.integration.chat.repo.ChatSubscriptionRepository
import xyz.om3lette.deadlines_api.data.integration.common.dto.IssuerContext
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.data.integration.common.response.IntegrationResult
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.requirePermissionGrpc
import xyz.om3lette.deadlines_api.util.jpaRepository.violatesConstraint
import java.time.Instant

@Service
class IntegrationSubscriptionService(
    private val userMessengerAccountRepository: UserMessengerAccountRepository,
    private val permissionService: PermissionService,
    private val integrationPermissionValidator: IntegrationPermissionValidator,
    private val organizationRepository: OrganizationRepository,
    private val chatRepository: ChatRepository,
    private val chatSubscriptionRepository: ChatSubscriptionRepository,
    private val deadlineRepository: DeadlineRepository,
    private val threadRepository: ThreadRepository,
    private val languageResolver: IntegrationLanguageResolver,
) {
    private fun getIssuerContext(messenger: Messenger, issuerMessengerAccountId: Long): IssuerContext {
        val messengerAccount = userMessengerAccountRepository.findByMessengerAndAccountId(
            messenger,
            issuerMessengerAccountId
        ).orElseThrow {
            grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.USER_NOT_FOUND,
                languageResolver.resolve(messenger, issuerMessengerAccountId)
            )
        }

        return IssuerContext(messenger, issuerMessengerAccountId, messengerAccount)
    }

    @Transactional
    fun subscribeToOrganization(
        issuerMessengerAccountId: Long,
        targetId: Long,
        messengerChatId: Long,
        messenger: Messenger,
        issuerHasMessengerChatAdminRights: Boolean = false,
    ): IntegrationResult = subscribeTo(
        issuerMessengerAccountId,
        messengerChatId,
        messenger,
        issuerHasMessengerChatAdminRights,
        ScopeType.ORGANIZATION,
    ) { issuer ->
        val organization = organizationRepository.findById(targetId).orElseThrow {
            grpcException(Status.NOT_FOUND, IntegrationResultKey.ORGANIZATION_NOT_FOUND, issuer.language)
        }
        requirePermissionGrpc(
            permissionService.hasAccess(
                issuer, OrganizationScope(organization.id, organization)
            ),
            IntegrationResultKey.ORGANIZATION_ACCESS_DENIED.value(),
            { issuer.language }
        )
        organization.id
    }

    @Transactional
    fun subscribeToThread(
        issuerMessengerAccountId: Long,
        targetId: Long,
        messengerChatId: Long,
        messenger: Messenger,
        issuerHasMessengerChatAdminRights: Boolean = false,
    ): IntegrationResult = subscribeTo(
        issuerMessengerAccountId,
        messengerChatId,
        messenger,
        issuerHasMessengerChatAdminRights,
        ScopeType.THREAD,
    ) { issuer ->
        val thread = threadRepository.findById(targetId).orElseThrow {
            grpcException(Status.NOT_FOUND, IntegrationResultKey.THREAD_NOT_FOUND, issuer.language)
        }
        requirePermissionGrpc(
            permissionService.hasAccess(issuer, ThreadScope(thread)),
            IntegrationResultKey.THREAD_ACCESS_DENIED.value(),
            { issuer.language }
        )
        thread.id
    }

    @Transactional
    fun subscribeToDeadline(
        issuerMessengerAccountId: Long,
        targetId: Long,
        messengerChatId: Long,
        messenger: Messenger,
        issuerHasMessengerChatAdminRights: Boolean = false,
    ): IntegrationResult = subscribeTo(
        issuerMessengerAccountId,
        messengerChatId,
        messenger,
        issuerHasMessengerChatAdminRights,
        ScopeType.DEADLINE,
    ) { issuer ->
        val deadline = deadlineRepository.findById(targetId).orElseThrow {
            grpcException(Status.NOT_FOUND, IntegrationResultKey.DEADLINE_NOT_FOUND, issuer.language)
        }
        requirePermissionGrpc(
            permissionService.hasAccess(issuer, DeadlineScope(deadline)),
            IntegrationResultKey.DEADLINE_ACCESS_DENIED.value(),
            { issuer.language }
        )
        deadline.id
    }

    private fun subscribeTo(
        issuerMessengerAccountId: Long,
        messengerChatId: Long,
        messenger: Messenger,
        issuerHasMessengerChatAdminRights: Boolean,
        scopeType: ScopeType,
        getTargetIdAndCheckPermission: (issuer: User) -> Long,
    ): IntegrationResult {
        val issuerContext = getIssuerContext(messenger, issuerMessengerAccountId)
        integrationPermissionValidator.requireChatManagementPermission(issuerContext, issuerHasMessengerChatAdminRights)
        val resolvedTargetId = getTargetIdAndCheckPermission(issuerContext.user)

        val chat = chatRepository.findByMessengerChatIdAndMessenger(messengerChatId, messenger)
            ?: throw grpcException(Status.NOT_FOUND, IntegrationResultKey.CHAT_NOT_FOUND, issuerContext.language)

        try {
            chatSubscriptionRepository.saveAndFlush(
                ChatSubscription(chat, resolvedTargetId, scopeType, Instant.now())
            )
        } catch (error: DataIntegrityViolationException) {
            if (!error.violatesConstraint(DatabaseConstraint.PK_CHAT_SUBSCRIPTIONS)) throw error
            throw grpcException(
                Status.ALREADY_EXISTS,
                IntegrationResultKey.SUBSCRIBE_ALREADY_SUBSCRIBED,
                chat.language,
                scopeType
            )
        }

        return integrationResult(IntegrationResultKey.SUBSCRIBE_SUCCESS, chat.language, scopeType)
    }

    @Transactional
    fun unsubscribeFromOrganization(
        issuerMessengerAccountId: Long,
        targetId: Long,
        messengerChatId: Long,
        messenger: Messenger,
        issuerHasMessengerChatAdminRights: Boolean = false,
    ): IntegrationResult = unsubscribeFrom(
        issuerMessengerAccountId,
        targetId,
        messengerChatId,
        messenger,
        issuerHasMessengerChatAdminRights,
        ScopeType.ORGANIZATION
    )

    @Transactional
    fun unsubscribeFromThread(
        issuerMessengerAccountId: Long,
        targetId: Long,
        messengerChatId: Long,
        messenger: Messenger,
        issuerHasMessengerChatAdminRights: Boolean = false,
    ): IntegrationResult = unsubscribeFrom(
        issuerMessengerAccountId,
        targetId,
        messengerChatId,
        messenger,
        issuerHasMessengerChatAdminRights,
        ScopeType.THREAD
    )

    @Transactional
    fun unsubscribeFromDeadline(
        issuerMessengerAccountId: Long,
        targetId: Long,
        messengerChatId: Long,
        messenger: Messenger,
        issuerHasMessengerChatAdminRights: Boolean = false,
    ): IntegrationResult = unsubscribeFrom(
        issuerMessengerAccountId,
        targetId,
        messengerChatId,
        messenger,
        issuerHasMessengerChatAdminRights,
        ScopeType.DEADLINE
    )

    private fun unsubscribeFrom(
        issuerMessengerAccountId: Long,
        targetId: Long,
        messengerChatId: Long,
        messenger: Messenger,
        issuerHasMessengerChatAdminRights: Boolean,
        scopeType: ScopeType,
    ): IntegrationResult {
        val issuerContext = getIssuerContext(messenger, issuerMessengerAccountId)
        integrationPermissionValidator.requireChatManagementPermission(issuerContext, issuerHasMessengerChatAdminRights)
        val chat = chatRepository.findByMessengerChatIdAndMessenger(messengerChatId, messenger)
            ?: throw grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.CHAT_NOT_FOUND,
                issuerContext.language
            )

        val deleted = deleteSubscriptions(chat, targetId, scopeType)

        return integrationResult(
            if (deleted > 0) IntegrationResultKey.UNSUBSCRIBE_SUCCESS else IntegrationResultKey.UNSUBSCRIBE_NOT_SUBSCRIBED,
            chat.language,
            scopeType
        )
    }

    @Transactional
    fun unsubscribeFromAll(
        issuerMessengerAccountId: Long,
        messengerChatId: Long,
        messenger: Messenger,
        issuerHasMessengerChatAdminRights: Boolean = false,
    ): IntegrationResult {
        val issuerContext = getIssuerContext(messenger, issuerMessengerAccountId)
        integrationPermissionValidator.requireChatManagementPermission(issuerContext, issuerHasMessengerChatAdminRights)
        val chat = chatRepository.findByMessengerChatIdAndMessenger(messengerChatId, messenger)
            ?: throw grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.CHAT_NOT_FOUND,
                issuerContext.language
            )

        chatSubscriptionRepository.deleteAllByChat(chat)
        return integrationResult(IntegrationResultKey.UNSUBSCRIBE_ALL_SUCCESS, chat.language)
    }

    private fun deleteSubscriptions(chat: Chat, scopeId: Long, scopeType: ScopeType): Int {
        var deleted = chatSubscriptionRepository.deleteByChatAndScopeIdAndScopeType(chat, scopeId, scopeType)

        when (scopeType) {
            ScopeType.ORGANIZATION -> {
                deleted += deleteSubscriptionsByScopeIds(
                    chat,
                    ScopeType.THREAD,
                    threadRepository.findAllIdsByOrganizationId(scopeId)
                )
                deleted += deleteSubscriptionsByScopeIds(
                    chat,
                    ScopeType.DEADLINE,
                    deadlineRepository.findAllIdsByOrganizationId(scopeId)
                )
            }
            ScopeType.THREAD -> {
                deleted += deleteSubscriptionsByScopeIds(
                    chat,
                    ScopeType.DEADLINE,
                    deadlineRepository.findAllIdsByThreadId(scopeId)
                )
            }
            ScopeType.DEADLINE -> Unit
        }

        return deleted
    }

    private fun deleteSubscriptionsByScopeIds(
        chat: Chat,
        scopeType: ScopeType,
        scopeIds: List<Long>
    ): Int {
        if (scopeIds.isEmpty()) return 0
        return chatSubscriptionRepository.deleteAllByChatAndScopeTypeAndScopeIdIn(chat, scopeType, scopeIds)
    }
}
