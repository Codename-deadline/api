package xyz.om3lette.deadlines_api.services.integration

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.proto.*

@GrpcService
class IntegrationInternalService(
    private val messengerAccountLinkingService: MessengerAccountLinkingService,
    private val integrationChatService: IntegrationChatService,
    private val integrationSubscriptionService: IntegrationSubscriptionService,
    private val languageResolver: IntegrationLanguageResolver,
) : IntegrationServiceGrpc.IntegrationServiceImplBase() {

    private val logger = LoggerFactory.getLogger(IntegrationInternalService::class.java)

    private fun getMessengerOr500(protoMessenger: ProtoMessenger): Messenger {
        val messenger = Messenger.getByValue(protoMessenger.ordinal)
        if (messenger == null) {
            logger.error("Messenger with ordinal: ${protoMessenger.ordinal} does not exist")
            throw grpcException(
                Status.INTERNAL,
                IntegrationResultKey.SERVER_INTERNAL,
                languageResolver.fallbackLanguage
            )
        }
        return messenger
    }

    override fun linkMessengerAccount(
        request: LinkMessengerAccountRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        messengerAccountLinkingService.linkMessengerAccount(
            request.requestId,
            request.isAccepted,
            request.messengerAccountId,
            getMessengerOr500(request.messenger)
        )
    )

    override fun subscribeToOrganization(
        request: SubscribeToRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationSubscriptionService.subscribeToOrganization(
            request.issuerMessengerAccountId,
            request.targetId,
            request.messengerChatId,
            getMessengerOr500(request.messenger),
            request.issuerHasMessengerChatAdminRights
        )
    )

    override fun subscribeToThread(
        request: SubscribeToRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationSubscriptionService.subscribeToThread(
            request.issuerMessengerAccountId,
            request.targetId,
            request.messengerChatId,
            getMessengerOr500(request.messenger),
            request.issuerHasMessengerChatAdminRights
        )
    )

    override fun subscribeToDeadline(
        request: SubscribeToRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationSubscriptionService.subscribeToDeadline(
            request.issuerMessengerAccountId,
            request.targetId,
            request.messengerChatId,
            getMessengerOr500(request.messenger),
            request.issuerHasMessengerChatAdminRights
        )
    )

    override fun unsubscribeFromOrganization(
        request: UnsubscribeFromRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationSubscriptionService.unsubscribeFromOrganization(
            request.issuerMessengerAccountId,
            request.targetId,
            request.messengerChatId,
            getMessengerOr500(request.messenger),
            request.issuerHasMessengerChatAdminRights
        )
    )

    override fun unsubscribeFromThread(
        request: UnsubscribeFromRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationSubscriptionService.unsubscribeFromThread(
            request.issuerMessengerAccountId,
            request.targetId,
            request.messengerChatId,
            getMessengerOr500(request.messenger),
            request.issuerHasMessengerChatAdminRights
        )
    )

    override fun unsubscribeFromDeadline(
        request: UnsubscribeFromRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationSubscriptionService.unsubscribeFromDeadline(
            request.issuerMessengerAccountId,
            request.targetId,
            request.messengerChatId,
            getMessengerOr500(request.messenger),
            request.issuerHasMessengerChatAdminRights
        )
    )

    override fun unsubscribeFromAll(
        request: UnsubscribeFromAllRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationSubscriptionService.unsubscribeFromAll(
            request.issuerMessengerAccountId,
            request.messengerChatId,
            getMessengerOr500(request.messenger),
            request.issuerHasMessengerChatAdminRights
        )
    )

    override fun updateChatInfo(
        request: UpdateChatInfoRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationChatService.updateChatInfo(
            request.issuerMessengerAccountId,
            getMessengerOr500(request.messenger),
            request.messengerChatId,
            if (request.hasLanguage()) Language.valueOf(request.language.name) else null,
            if (request.hasTitle()) request.title else null,
            request.issuerHasMessengerChatAdminRights
        )
    )

    override fun registerChat(
        request: RegisterChatRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationChatService.registerChat(
            request.botId,
            request.issuerMessengerAccountId,
            getMessengerOr500(request.messenger),
            request.messengerChatId,
            request.chatTitle,
            request.language,
            request.issuerHasMessengerChatAdminRights
        )
    )

    override fun deregisterChat(
        request: DeregisterChatRequest,
        responseObserver: StreamObserver<GeneralResponse>
    ) = responseObserver.sendResult(
        integrationChatService.deregisterChat(
            request.messengerChatId,
            getMessengerOr500(request.messenger),
            request.issuerMessengerAccountId,
            request.issuerHasMessengerChatAdminRights
        )
    )
}
