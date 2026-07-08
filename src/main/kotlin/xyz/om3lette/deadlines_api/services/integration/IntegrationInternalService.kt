package xyz.om3lette.deadlines_api.services.integration

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.grpc.server.service.GrpcService
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.bot.repo.BotRepository
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.chat.model.ChatSubscription
import xyz.om3lette.deadlines_api.data.integration.chat.repo.ChatRepository
import xyz.om3lette.deadlines_api.data.integration.chat.repo.ChatSubscriptionRepository
import xyz.om3lette.deadlines_api.data.integration.common.response.IntegrationResult
import xyz.om3lette.deadlines_api.data.integration.common.dto.IssuerContext
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.data.integration.constraints.IntegrationConstraints
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.proto.DeregisterChatRequest
import xyz.om3lette.deadlines_api.proto.GeneralResponse
import xyz.om3lette.deadlines_api.proto.IntegrationServiceGrpc
import xyz.om3lette.deadlines_api.proto.LinkMessengerAccountRequest
import xyz.om3lette.deadlines_api.proto.ProtoMessenger
import xyz.om3lette.deadlines_api.proto.RegisterChatRequest
import xyz.om3lette.deadlines_api.proto.SubscribeToRequest
import xyz.om3lette.deadlines_api.proto.UnsubscribeFromAllRequest
import xyz.om3lette.deadlines_api.proto.UnsubscribeFromRequest
import xyz.om3lette.deadlines_api.proto.UpdateChatInfoRequest
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.repo.AccountLinkageRepository
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.requirePermissionGrpc
import java.time.Instant

@GrpcService
class IntegrationInternalService(
    private val userMessengerAccountRepository: UserMessengerAccountRepository,
    private val permissionService: PermissionService,
    private val organizationRepository: OrganizationRepository,
    private val chatRepository: ChatRepository,
    private val chatSubscriptionRepository: ChatSubscriptionRepository,
    private val botRepository: BotRepository,
    private val userRepository: UserRepository,
    private val deadlineRepository: DeadlineRepository,
    private val threadRepository: ThreadRepository,
    private val accountLinkageRepository: AccountLinkageRepository,
    private val languageResolver: IntegrationLanguageResolver,
) : IntegrationServiceGrpc.IntegrationServiceImplBase() {

    private val logger = LoggerFactory.getLogger(IntegrationService::class.java)

    private fun grpcException(
        status: Status,
        key: IntegrationResultKey,
        language: Language = Language.EN,
        scopeType: ScopeType? = null,
    ) = GrpcKeyLocaleException(status, key.value(scopeType), language)

    private fun StreamObserver<GeneralResponse>.sendResult(
        key: IntegrationResultKey,
        language: Language = Language.EN,
        scopeType: ScopeType? = null,
    ) {
        onNext(IntegrationResult(key.value(scopeType), language).toResponse())
        onCompleted()
    }

    private fun getIssuerContext(messenger: Messenger, accountId: Long, notFoundKey: IntegrationResultKey): IssuerContext {
        val messengerAccount = userMessengerAccountRepository.findByMessengerAndAccountId(messenger, accountId).orElseThrow {
            grpcException(Status.NOT_FOUND, notFoundKey, languageResolver.resolve(messenger, accountId))
        }

        return IssuerContext(messenger, accountId, messengerAccount)
    }

    private fun getMessengerOr500(protoMessenger: ProtoMessenger): Messenger {
        val messenger = Messenger.getByValue(protoMessenger.ordinal)
        if (messenger == null) {
            logger.error("Messenger with ordinal: ${protoMessenger.ordinal} does not exist")
            throw grpcException(Status.INTERNAL, IntegrationResultKey.SERVER_INTERNAL)
        }
        return messenger
    }

    override fun linkMessengerAccount(
        request: LinkMessengerAccountRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) {
        // No permission check needed as the request was created => permission was granted
        // TODO: Use user's preferred language
        val linkAccountRequest = accountLinkageRepository.findById(request.requestId).orElseThrow {
            grpcException(Status.NOT_FOUND, IntegrationResultKey.REQUEST_NOT_FOUND)
        }

        accountLinkageRepository.delete(linkAccountRequest)
        if (!request.isAccepted) {
            logger.info("Account linkage request ${request.requestId} declined")
            responseObserver.sendResult(IntegrationResultKey.ACCOUNT_LINKAGE_IGNORED)
            return
        }

        val user = userRepository.findById(linkAccountRequest.userId).orElseThrow {
            grpcException(Status.NOT_FOUND, IntegrationResultKey.USER_NOT_FOUND)
        }
        userMessengerAccountRepository.save(
            UserMessengerAccount(
                0,
                user,
                linkAccountRequest.accountId,
                linkAccountRequest.messenger
            )
        )
        logger.info("Account linkage request ${request.requestId} accepted")

        responseObserver.sendResult(IntegrationResultKey.ACCOUNT_LINKAGE_SUCCESS, user.language)
    }

    private fun subscribeTo(
        request: SubscribeToRequest,
        scopeType: ScopeType,
        responseObserver: StreamObserver<GeneralResponse>,
        getTargetIdAndCheckPermission: (issuer: User) -> Long
    ) {
        // TODO: Allows to subscribe to the lower level entities when a sub for a higher level is in place
        // Not a braking problem as deduplication will happen when moving to outbox, but takes up space in db
        // but might be worth rethinking
        val messenger = getMessengerOr500(request.messenger)
        val issuerContext = getIssuerContext(
            messenger, request.issuerAccountId, IntegrationResultKey.USER_NOT_FOUND
        )
        val targetId: Long = getTargetIdAndCheckPermission(issuerContext.user)

        val chat = chatRepository.findByMessengerChatIdAndMessenger(request.messengerChatId, messenger) ?:
            throw grpcException(Status.NOT_FOUND, IntegrationResultKey.CHAT_NOT_FOUND, issuerContext.language)

        try {
            chatSubscriptionRepository.save(
                ChatSubscription(
                    0, chat, targetId, scopeType, Instant.now()
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw grpcException(
                Status.ALREADY_EXISTS,
                IntegrationResultKey.SUBSCRIBE_ALREADY_SUBSCRIBED,
                chat.language,
                scopeType
            )
        }

        responseObserver.sendResult(IntegrationResultKey.SUBSCRIBE_SUCCESS, chat.language, scopeType)
    }

    override fun subscribeToOrganization(
        request: SubscribeToRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = subscribeTo(request, ScopeType.ORGANIZATION, responseObserver) { issuer ->
        val organization = organizationRepository.findById(request.targetId).orElseThrow {
            grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.ORGANIZATION_NOT_FOUND,
                issuer.language
            )
        }
        requirePermissionGrpc(
            permissionService.hasAccess(issuer, OrganizationScope(
                organization.id,organization
            )),
            IntegrationResultKey.ORGANIZATION_ACCESS_DENIED.value(),
            { issuer.language }
        )
        organization.id
    }

    override fun subscribeToThread(
        request: SubscribeToRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = subscribeTo(request, ScopeType.THREAD, responseObserver) { issuer ->
        val thread = threadRepository.findById(request.targetId).orElseThrow {
            grpcException(Status.NOT_FOUND, IntegrationResultKey.THREAD_NOT_FOUND, issuer.language)
        }
        requirePermissionGrpc(
            permissionService.hasAccess(issuer, ThreadScope(thread)),
            IntegrationResultKey.THREAD_ACCESS_DENIED.value(),
            { issuer.language }
        )
        thread.id
    }

    override fun subscribeToDeadline(
        request: SubscribeToRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = subscribeTo(request, ScopeType.DEADLINE, responseObserver) { issuer ->
        val deadline = deadlineRepository.findById(request.targetId).orElseThrow {
            grpcException(Status.NOT_FOUND, IntegrationResultKey.DEADLINE_NOT_FOUND, issuer.language)
        }
        requirePermissionGrpc(
            permissionService.hasAccess(issuer, DeadlineScope(deadline)),
            IntegrationResultKey.DEADLINE_ACCESS_DENIED.value(),
            { issuer.language }
        )
        deadline.id
    }

    private fun unsubscribeFrom(
        request: UnsubscribeFromRequest,
        scopeType: ScopeType,
        responseObserver: StreamObserver<GeneralResponse>
    ) {
        val messenger = getMessengerOr500(request.messenger)
        val chat = chatRepository.findByMessengerChatIdAndMessenger(request.messengerChatId, messenger) ?:
            throw grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.CHAT_NOT_FOUND,
                languageResolver.resolve(messenger, request.issuerAccountId)
            )

        val deleted = deleteSubscriptions(chat, request.targetId, scopeType)

        responseObserver.sendResult(
            if (deleted > 0) IntegrationResultKey.UNSUBSCRIBE_SUCCESS else IntegrationResultKey.UNSUBSCRIBE_NOT_SUBSCRIBED,
            chat.language,
            scopeType
        )
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

    @Transactional
    override fun unsubscribeFromOrganization(
        request: UnsubscribeFromRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = unsubscribeFrom(request, ScopeType.ORGANIZATION, responseObserver)

    @Transactional
    override fun unsubscribeFromThread(
        request: UnsubscribeFromRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = unsubscribeFrom(request, ScopeType.THREAD, responseObserver)

    @Transactional
    override fun unsubscribeFromDeadline(
        request: UnsubscribeFromRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = unsubscribeFrom(request, ScopeType.DEADLINE, responseObserver)

    @Transactional
    override fun unsubscribeFromAll(
        request: UnsubscribeFromAllRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) {
        val messenger = getMessengerOr500(request.messenger)

        val chat = chatRepository.findByMessengerChatIdAndMessenger(request.messengerChatId, messenger) ?:
            throw grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.CHAT_NOT_FOUND,
                languageResolver.resolve(messenger, request.issuerAccountId)
            )

        val deleted: Int = chatSubscriptionRepository.deleteAllByChat(chat)

        logger.info("Removed all chat's ${chat.id} subscriptions: $deleted")
        responseObserver.sendResult(IntegrationResultKey.UNSUBSCRIBE_ALL_SUCCESS, chat.language)
    }

    override fun updateChatInfo(request: UpdateChatInfoRequest, responseObserver: StreamObserver<GeneralResponse>) {
        val messenger = getMessengerOr500(request.messenger)
        val chat = chatRepository.findByMessengerChatIdAndMessenger(request.messengerChatId, messenger) ?:
            throw grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.CHAT_NOT_FOUND,
                languageResolver.resolve(messenger, request.issuerAccountId)
            )

        if (request.hasLanguage()) chat.language = Language.valueOf(request.language.name)
        if (request.hasTitle()) chat.title = request.title.take(IntegrationConstraints.CHAT_TITLE_MAX)
        chatRepository.save(chat)

        responseObserver.sendResult(IntegrationResultKey.CHAT_INFO_UPDATE_SUCCESS, chat.language)
    }

    override fun registerChat(
        request: RegisterChatRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) {
        val messenger = getMessengerOr500(request.messenger)
        val issuerContext = getIssuerContext(
            messenger,
            request.issuerAccountId,
            IntegrationResultKey.LINKED_ACCOUNT_NOT_FOUND
        )

       requirePermissionGrpc(
           permissionService.canRegisterChat(issuerContext.user),
           IntegrationResultKey.CHAT_REGISTRATION_DENIED.value(),
           { issuerContext.language }
       )

        val language = Language.entries.firstOrNull { it.name == request.language } ?: issuerContext.language

        val bot = botRepository.findByBotIdAndMessenger(request.botId, messenger).orElseThrow {
            logger.error("Bot with id ${request.botId} in messenger ${messenger.name} not found")
            grpcException(Status.INTERNAL, IntegrationResultKey.SERVER_INTERNAL, language)
        }

        try {
            chatRepository.save(
                Chat(
                    0,
                    request.messengerChatId,
                    bot.messenger,
                    request.chatTitle.take(IntegrationConstraints.CHAT_TITLE_MAX),
                    bot,
                    language,
                    Instant.now()
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw grpcException(Status.ALREADY_EXISTS, IntegrationResultKey.CHAT_ALREADY_REGISTERED, language)
        }
        responseObserver.sendResult(IntegrationResultKey.REGISTER_CHAT_SUCCESS, language)
    }

    @Transactional
    override fun deregisterChat(request: DeregisterChatRequest, responseObserver: StreamObserver<GeneralResponse>) {
        val messenger = getMessengerOr500(request.messenger)
        val chatToDelete = chatRepository.findByMessengerChatIdAndMessenger(
            request.messengerChatId,
            messenger
        )

        if (chatToDelete != null) chatRepository.delete(chatToDelete)

        responseObserver.sendResult(
            if (chatToDelete != null) {
                IntegrationResultKey.DEREGISTER_CHAT_SUCCESS
            } else {
                IntegrationResultKey.DEREGISTER_CHAT_NOT_REGISTERED
            },
            languageResolver.resolve(chatToDelete, messenger, null)
        )
    }
}
