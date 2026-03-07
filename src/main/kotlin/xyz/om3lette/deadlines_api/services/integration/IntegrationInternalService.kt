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
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.proto.DeregisterChatRequest
import xyz.om3lette.deadlines_api.proto.GeneralResponse
import xyz.om3lette.deadlines_api.proto.IntegrationServiceGrpc
import xyz.om3lette.deadlines_api.proto.LinkMessengerAccountRequest
import xyz.om3lette.deadlines_api.proto.Locale
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
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

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
) : IntegrationServiceGrpc.IntegrationServiceImplBase() {

    private val logger = LoggerFactory.getLogger(IntegrationService::class.java)

//    TODO: Write to redis for faster retrieval
    private fun getLanguageByAccountId(issuerAccountId: Long?): Language {
        if (issuerAccountId == null) return Language.EN
        return userMessengerAccountRepository.findById(issuerAccountId).getOrNull()?.user?.language ?: Language.EN
    }

    private fun getMessengerOr400(protoMessenger: ProtoMessenger): Messenger {
        val messenger = Messenger.getByValue(protoMessenger.ordinal)
        if (messenger == null) {
            logger.error("Messenger with ordinal: ${protoMessenger.ordinal} does not exist")
            throw GrpcKeyLocaleException(Status.INTERNAL, "server_internal")
        }
        return messenger
    }

    override fun linkMessengerAccount(
        request: LinkMessengerAccountRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) {
//      No permission check needed as the request was created -> permission was granted
//      TODO: Use user's preferred language
        val linkAccountRequest = accountLinkageRepository.findById(request.requestId).orElseThrow {
            GrpcKeyLocaleException(Status.NOT_FOUND, "request_not_found", getLanguageByAccountId(null))
        }

        accountLinkageRepository.delete(linkAccountRequest)
        if (!request.isAccepted) {
            logger.info("Account linkage request ${request.requestId} declined")
            responseObserver.onNext(
                GeneralResponse.newBuilder().setKey("account_linkage.ignored").build()
            )
            responseObserver.onCompleted()
            return
        }

        val user = userRepository.findById(linkAccountRequest.userId).orElseThrow {
            GrpcKeyLocaleException(Status.NOT_FOUND, "errors.user_not_found", getLanguageByAccountId(null))
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

        responseObserver.onNext(
            GeneralResponse.newBuilder()
                .setKey("account_linkage.success")
                .setLocale(Locale.EN)
                .build()
        )
        responseObserver.onCompleted()
    }

    private fun subscribeTo(
        request: SubscribeToRequest,
        scopeType: ScopeType,
        responseObserver: StreamObserver<GeneralResponse>,
        getTargetIdAndCheckPermission: (issuer: User) -> Long
    ) {
//      FIXME: Allows to subscribe to the lower level entities when a sub for a higher level is in place
//      Not a braking problem as deduplication will happen when moving to outbox, but takes up space in db
//      See xyz/om3lette/deadlines_api/data/notifications/repo/impl/DeadlineNotificationCustomRepositoryImpl.kt:62
        val messenger = getMessengerOr400(request.messenger)
        val issuer = userMessengerAccountRepository.findByMessengerAndAccountId(
            messenger, request.issuerAccountId
        ).orElseThrow {
            GrpcKeyLocaleException(
                Status.NOT_FOUND, "errors.user_not_found", getLanguageByAccountId(null)
            )
        }.user
        val targetId: Long = getTargetIdAndCheckPermission(issuer)

        val chat = chatRepository.findByMessengerChatIdAndMessenger(request.messengerChatId, messenger) ?:
            throw GrpcKeyLocaleException(Status.NOT_FOUND, "errors.chat_not_found", issuer.language)

        try {
            chatSubscriptionRepository.save(
                ChatSubscription(
                    0, chat, targetId, scopeType, Instant.now()
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw GrpcKeyLocaleException(
                Status.ALREADY_EXISTS,
                "sub.${scopeType.name.lowercase()}.already_subscribed",
                chat.language
            )
        }

        responseObserver.onNext(
            GeneralResponse.newBuilder()
                .setKey("sub.${scopeType.name.lowercase()}.success")
                .setLocale(Locale.valueOf(chat.language.name))
                .build()
        )
        responseObserver.onCompleted()
    }

    override fun subscribeToOrganization(
        request: SubscribeToRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = subscribeTo(request, ScopeType.ORGANIZATION, responseObserver) { issuer ->
        val organization = organizationRepository.findById(request.targetId).orElseThrow {
            GrpcKeyLocaleException(Status.NOT_FOUND, "errors.organization_not_found", getLanguageByAccountId(request.issuerAccountId))
        }
        requirePermissionGrpc(
            permissionService.hasOrganizationAccess(issuer, organization),
            "error.organization_access_denied",
            { getLanguageByAccountId(request.issuerAccountId) }
        )
        organization.id
    }

    override fun subscribeToThread(
        request: SubscribeToRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = subscribeTo(request, ScopeType.THREAD, responseObserver) { issuer ->
        val thread: Optional<Thread> = threadRepository.findById(request.targetId)
        if (thread.isEmpty) {
            throw GrpcKeyLocaleException(
                Status.NOT_FOUND,
                "errors.thread_not_found",
                getLanguageByAccountId(request.issuerAccountId)
            )
        }
        requirePermissionGrpc(
            permissionService.hasThreadAccess(issuer, thread.get()),
            "errors.thread_access_denied",
            { getLanguageByAccountId(request.issuerAccountId) }
        )
        thread.get().id
    }

    override fun subscribeToDeadline(
        request: SubscribeToRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = subscribeTo(request, ScopeType.DEADLINE, responseObserver) { issuer ->
        val deadline: Optional<Deadline> = deadlineRepository.findById(request.targetId)
        if (deadline.isEmpty) {
            throw GrpcKeyLocaleException(
                Status.NOT_FOUND,
                "errors.deadline_not_found",
                getLanguageByAccountId(request.issuerAccountId)
            )
        }
        requirePermissionGrpc(
            permissionService.hasDeadlineAccess(issuer, deadline.get()),
            "errors.deadline_access_denied",
            { getLanguageByAccountId(request.issuerAccountId) }
        )
        deadline.get().id
    }

    private fun unsubscribeFrom(
        request: UnsubscribeFromRequest,
        scopeType: ScopeType,
        responseObserver: StreamObserver<GeneralResponse>
    ) {
        val messenger = getMessengerOr400(request.messenger)

        val chat = chatRepository.findByMessengerChatIdAndMessenger(request.messengerChatId, messenger) ?:
            throw GrpcKeyLocaleException(
                Status.NOT_FOUND,
                "errors.chat_not_found",
                getLanguageByAccountId(request.issuerAccountId)
            )

        val isDeleted = chatSubscriptionRepository.deleteByChatAndScopeId(chat, request.targetId)

        val keyPostfix = if (isDeleted) "success" else "not_subscribed"
        responseObserver.onNext(
            GeneralResponse.newBuilder()
                .setKey("unsub.${scopeType.name.lowercase()}.$keyPostfix")
                .setLocale(Locale.valueOf(chat.language.name))
                .build()
        )
        responseObserver.onCompleted()
    }

    override fun unsubscribeFromOrganization(
        request: UnsubscribeFromRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = unsubscribeFrom(request, ScopeType.ORGANIZATION, responseObserver)

    override fun unsubscribeFromThread(
        request: UnsubscribeFromRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = unsubscribeFrom(request, ScopeType.THREAD, responseObserver)

    override fun unsubscribeFromDeadline(
        request: UnsubscribeFromRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = unsubscribeFrom(request, ScopeType.DEADLINE, responseObserver)

    override fun unsubscribeFromAll(
        request: UnsubscribeFromAllRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) {
        val messenger = getMessengerOr400(request.messenger)

        val chat = chatRepository.findByMessengerChatIdAndMessenger(request.messengerChatId, messenger) ?:
            throw GrpcKeyLocaleException(
                Status.NOT_FOUND,
                "errors.chat_not_found",
                getLanguageByAccountId(request.issuerAccountId)
            )

        val deleted: Int = chatSubscriptionRepository.deleteAllByChat(chat)

        logger.info("Removed all chat's ${chat.id} subscriptions: $deleted")
        responseObserver.onNext(
            GeneralResponse.newBuilder()
                .setKey("unsub.all.success")
                .setLocale(Locale.valueOf(chat.language.name))
                .build()
        )
        responseObserver.onCompleted()
    }

    override fun updateChatInfo(request: UpdateChatInfoRequest, responseObserver: StreamObserver<GeneralResponse>) {
        // TODO: Use user's preferred language
        val messenger = Messenger.valueOf(request.messenger.name)
        val chat = chatRepository.findByMessengerChatIdAndMessenger(request.messengerChatId, messenger) ?:
            throw GrpcKeyLocaleException(
                Status.NOT_FOUND,
                "errors.chat_not_found",

            )

        if (request.language != null) chat.language = Language.valueOf(request.language.name)
        if (request.title != null) chat.title = request.title.take(256)
        chatRepository.save(chat)

        responseObserver.onNext(
            GeneralResponse.newBuilder()
                .setKey("chat_info_update.success")
                .setLocale(request.language)
                .build()
        )
        responseObserver.onCompleted()
    }

    override fun registerChat(
        request: RegisterChatRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) {
        val messenger = getMessengerOr400(request.messenger)
        userMessengerAccountRepository.findByMessengerAndAccountId(
            messenger, request.issuerAccountId
        ).orElseThrow {
            GrpcKeyLocaleException(
                Status.NOT_FOUND,
                "errors.linked_account_not_found",
                getLanguageByAccountId(null)
            )
        }

//       Currently always returns true
//        requirePermissionGrpc(
//            permissionService.canRegisterChat(user)
//        )

        val language = Language.valueOf(request.language)

        val bot = botRepository.findByBotIdAndMessenger(request.botId, messenger).orElseThrow {
            logger.error("Bot with id ${request.botId} in messenger ${messenger.name} not found")
            GrpcKeyLocaleException(Status.INTERNAL, "errors.server_iternal", language)
        }

        try {
            chatRepository.save(
                Chat(
                    0,
                    request.messengerChatId,
                    bot.messenger,
                    request.chatTitle.take(256),
                    bot,
                    language,
                    Instant.now()
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw GrpcKeyLocaleException(Status.ALREADY_EXISTS, "errors.chat_already_registered", language)
        }
        responseObserver.onNext(
            GeneralResponse.newBuilder()
                .setKey("register_chat.success")
                .setLocale(Locale.valueOf(language.name))
                .build()
        )
        responseObserver.onCompleted()
    }

    @Transactional
    override fun deregisterChat(request: DeregisterChatRequest, responseObserver: StreamObserver<GeneralResponse>) {
//       Used to retrieve language
        val chatToDelete = chatRepository.findByMessengerChatIdAndMessenger(
            request.messengerChatId,
            Messenger.valueOf(request.messenger.name)
        )

        if (chatToDelete != null) chatRepository.delete(chatToDelete)

        responseObserver.onNext(
            GeneralResponse.newBuilder()
                .setKey(
                    if (chatToDelete != null) "deregister_chat.success" else "deregister_chat.not_registered"
                )
                .setLocale(
                    Locale.valueOf((chatToDelete?.language ?: Language.EN).name)
                )
                .build()
        )
        responseObserver.onCompleted()
    }
}