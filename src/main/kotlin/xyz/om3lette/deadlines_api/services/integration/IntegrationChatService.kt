package xyz.om3lette.deadlines_api.services.integration

import io.grpc.Status
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.bot.repo.BotRepository
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.chat.repo.ChatRepository
import xyz.om3lette.deadlines_api.data.integration.common.dto.IssuerContext
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.data.integration.common.response.IntegrationResult
import xyz.om3lette.deadlines_api.data.integration.constraints.IntegrationConstraints
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.util.jpaRepository.violatesConstraint
import java.time.Instant

@Service
class IntegrationChatService(
    private val userMessengerAccountRepository: UserMessengerAccountRepository,
    private val integrationPermissionValidator: IntegrationPermissionValidator,
    private val botRepository: BotRepository,
    private val chatRepository: ChatRepository,
    private val languageResolver: IntegrationLanguageResolver,
) {
    private val logger = LoggerFactory.getLogger(IntegrationChatService::class.java)

    private fun getIssuerContext(messenger: Messenger, issuerMessengerAccountId: Long): IssuerContext {
        val messengerAccount = userMessengerAccountRepository.findByMessengerAndAccountId(
            messenger,
            issuerMessengerAccountId
        ).orElseThrow {
            grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.LINKED_ACCOUNT_NOT_FOUND,
                languageResolver.resolve(messenger, issuerMessengerAccountId)
            )
        }

        return IssuerContext(messenger, issuerMessengerAccountId, messengerAccount)
    }

    @Transactional
    fun registerChat(
        botId: Long,
        issuerMessengerAccountId: Long,
        messenger: Messenger,
        messengerChatId: Long,
        chatTitle: String,
        languageName: String,
        issuerHasMessengerChatAdminRights: Boolean,
    ): IntegrationResult {
        val issuerContext = getIssuerContext(messenger, issuerMessengerAccountId)
        integrationPermissionValidator.requireChatManagementPermission(issuerContext, issuerHasMessengerChatAdminRights)

        val language = Language.entries.firstOrNull { it.name == languageName } ?: issuerContext.language

        val bot = botRepository.findByBotIdAndMessenger(botId, messenger).orElseThrow {
            logger.error("Bot with id $botId in messenger ${messenger.name} not found")
            grpcException(Status.INTERNAL, IntegrationResultKey.SERVER_INTERNAL, language)
        }

        try {
            chatRepository.saveAndFlush(
                Chat(
                    0,
                    messengerChatId,
                    bot.messenger,
                    chatTitle.take(IntegrationConstraints.CHAT_TITLE_MAX),
                    bot,
                    language,
                    Instant.now()
                )
            )
        } catch (error: DataIntegrityViolationException) {
            if (!error.violatesConstraint(DatabaseConstraint.UQ_CHATS_MESSENGER_CHAT_ID_MESSENGER)) throw error
            throw grpcException(Status.ALREADY_EXISTS, IntegrationResultKey.CHAT_ALREADY_REGISTERED, language)
        }

        return integrationResult(IntegrationResultKey.REGISTER_CHAT_SUCCESS, language)
    }

    @Transactional
    fun deregisterChat(
        messengerChatId: Long,
        messenger: Messenger,
        issuerMessengerAccountId: Long,
        issuerHasMessengerChatAdminRights: Boolean,
    ): IntegrationResult {
        val issuerContext = getIssuerContext(messenger, issuerMessengerAccountId)
        integrationPermissionValidator.requireChatManagementPermission(issuerContext, issuerHasMessengerChatAdminRights)

        val chatToDelete = chatRepository.findByMessengerChatIdAndMessenger(messengerChatId, messenger)
        if (chatToDelete != null) chatRepository.delete(chatToDelete)

        return integrationResult(
            if (chatToDelete != null) {
                IntegrationResultKey.DEREGISTER_CHAT_SUCCESS
            } else {
                IntegrationResultKey.DEREGISTER_CHAT_NOT_REGISTERED
            },
            chatToDelete?.language ?: issuerContext.language
        )
    }

    @Transactional
    fun updateChatInfo(
        issuerMessengerAccountId: Long,
        messenger: Messenger,
        messengerChatId: Long,
        language: Language?,
        title: String?,
        issuerHasMessengerChatAdminRights: Boolean,
    ): IntegrationResult {
        val issuerContext = getIssuerContext(messenger, issuerMessengerAccountId)
        integrationPermissionValidator.requireChatManagementPermission(issuerContext, issuerHasMessengerChatAdminRights)

        val chat = chatRepository.findByMessengerChatIdAndMessenger(messengerChatId, messenger)
            ?: throw grpcException(
                Status.NOT_FOUND,
                IntegrationResultKey.CHAT_NOT_FOUND,
                issuerContext.language
            )

        if (language != null) chat.language = language
        if (title != null) chat.title = title.take(IntegrationConstraints.CHAT_TITLE_MAX)
        chatRepository.save(chat)

        return integrationResult(IntegrationResultKey.CHAT_INFO_UPDATE_SUCCESS, chat.language)
    }
}
