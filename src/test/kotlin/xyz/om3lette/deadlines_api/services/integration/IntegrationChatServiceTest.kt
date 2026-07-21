package xyz.om3lette.deadlines_api.services.integration

import io.grpc.Status
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import xyz.om3lette.deadlines_api.db.constraintViolation
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.bot.repo.BotRepository
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.chat.repo.ChatRepository
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.data.integration.constraints.IntegrationConstraints
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntegrationChatServiceTest {
    private val userMessengerAccountRepository: UserMessengerAccountRepository = mockk()
    private val permissionService: PermissionService = mockk()
    // Serves as an extraction of common require clauses and maps directly to permission service
    private val integrationPermissionValidator: IntegrationPermissionValidator = IntegrationPermissionValidator(
        permissionService
    )
    private val botRepository: BotRepository = mockk()
    private val chatRepository: ChatRepository = mockk()
    private val languageResolver = IntegrationLanguageResolver(
        userMessengerAccountRepository,
        IntegrationTestFixtures.integrationProperties(fallbackLanguage = Language.RU)
    )
    private val service = IntegrationChatService(
        userMessengerAccountRepository,
        integrationPermissionValidator,
        botRepository,
        chatRepository,
        languageResolver
    )

    private val user = IntegrationTestFixtures.user(language = Language.EN)
    private val account = IntegrationTestFixtures.messengerAccount(user = user)
    private val bot = IntegrationTestFixtures.bot()

    @Test
    fun `registerChat saves chat when issuer has messenger chat admin rights`() {
        val savedChat = slot<Chat>()
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, true) } returns true
        every { botRepository.findByBotIdAndMessenger(IntegrationTestFixtures.BOT_ID, Messenger.TELEGRAM) } returns Optional.of(bot)
        every { chatRepository.saveAndFlush(capture(savedChat)) } answers { savedChat.captured }

        val result = service.registerChat(
            IntegrationTestFixtures.BOT_ID,
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            Messenger.TELEGRAM,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            "Chat title",
            Language.RU.name,
            issuerHasMessengerChatAdminRights = true
        )

        assertEquals(IntegrationResultKey.REGISTER_CHAT_SUCCESS.value(), result.key)
        assertEquals(Language.RU, result.language)
        assertEquals(IntegrationTestFixtures.MESSENGER_CHAT_ID, savedChat.captured.messengerChatId)
        assertEquals("Chat title", savedChat.captured.title)
        assertEquals(bot, savedChat.captured.bot)
        assertEquals(Language.RU, savedChat.captured.language)
    }

    @Test
    fun `registerChat allows API admin without messenger chat admin rights`() {
        val admin = IntegrationTestFixtures.admin(language = Language.EN)
        val adminAccount = IntegrationTestFixtures.messengerAccount(user = admin)
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(Messenger.TELEGRAM, IntegrationTestFixtures.ISSUER_ACCOUNT_ID)
        } returns Optional.of(adminAccount)
        every { permissionService.canManageIntegrationChat(admin, false) } returns true
        every { botRepository.findByBotIdAndMessenger(IntegrationTestFixtures.BOT_ID, Messenger.TELEGRAM) } returns Optional.of(bot)
        every { chatRepository.saveAndFlush(any()) } answers { firstArg() }

        val result = service.registerChat(
            IntegrationTestFixtures.BOT_ID,
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            Messenger.TELEGRAM,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            "Chat title",
            Language.EN.name,
            issuerHasMessengerChatAdminRights = false
        )

        assertEquals(IntegrationResultKey.REGISTER_CHAT_SUCCESS.value(), result.key)
    }

    @Test
    fun `registerChat uses issuer language when language string is invalid`() {
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, true) } returns true
        every { botRepository.findByBotIdAndMessenger(IntegrationTestFixtures.BOT_ID, Messenger.TELEGRAM) } returns Optional.of(bot)
        every { chatRepository.saveAndFlush(any()) } answers { firstArg() }

        val result = service.registerChat(
            IntegrationTestFixtures.BOT_ID,
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            Messenger.TELEGRAM,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            "Chat title",
            "UNKNOWN",
            issuerHasMessengerChatAdminRights = true
        )

        assertEquals(user.language, result.language)
    }

    @Test
    fun `registerChat truncates long title`() {
        val savedChat = slot<Chat>()
        everyAccountExists()
        every {
            permissionService.canManageIntegrationChat(user, true)
        } returns true
        every {
            botRepository.findByBotIdAndMessenger(IntegrationTestFixtures.BOT_ID, Messenger.TELEGRAM)
        } returns Optional.of(bot)
        every { chatRepository.saveAndFlush(capture(savedChat)) } answers { savedChat.captured }

        service.registerChat(
            IntegrationTestFixtures.BOT_ID,
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            Messenger.TELEGRAM,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            "x".repeat(IntegrationConstraints.CHAT_TITLE_MAX + 10),
            Language.EN.name,
            issuerHasMessengerChatAdminRights = true
        )

        assertEquals(IntegrationConstraints.CHAT_TITLE_MAX, savedChat.captured.title.length)
    }

    @Test
    fun `registerChat fails when linked account is missing`() {
        everyAccountMissing()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.registerChat(
                IntegrationTestFixtures.BOT_ID,
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                Messenger.TELEGRAM,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                "Chat title",
                Language.EN.name,
                issuerHasMessengerChatAdminRights = true
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.LINKED_ACCOUNT_NOT_FOUND.value(), exception.key)
        assertEquals(Language.RU, exception.language)
    }

    @Test
    fun `registerChat fails when permission is denied`() {
        everyAccountExists()
        every {
            permissionService.canManageIntegrationChat(user, false)
        } returns false

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.registerChat(
                IntegrationTestFixtures.BOT_ID,
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                Messenger.TELEGRAM,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                "Chat title",
                Language.EN.name,
                issuerHasMessengerChatAdminRights = false
            )
        }

        assertEquals(Status.PERMISSION_DENIED, exception.status)
        assertEquals(IntegrationResultKey.CHAT_MANAGEMENT_DENIED.value(), exception.key)
        assertEquals(user.language, exception.language)
    }

    @Test
    fun `registerChat fails when bot is missing`() {
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, true) } returns true
        every {
            botRepository.findByBotIdAndMessenger(IntegrationTestFixtures.BOT_ID, Messenger.TELEGRAM)
        } returns Optional.empty()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.registerChat(
                IntegrationTestFixtures.BOT_ID,
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                Messenger.TELEGRAM,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                "Chat title",
                Language.EN.name,
                issuerHasMessengerChatAdminRights = true
            )
        }

        assertEquals(Status.INTERNAL, exception.status)
        assertEquals(IntegrationResultKey.SERVER_INTERNAL.value(), exception.key)
    }

    @Test
    fun `registerChat fails when chat already exists`() {
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, true) } returns true
        every {
            botRepository.findByBotIdAndMessenger(IntegrationTestFixtures.BOT_ID, Messenger.TELEGRAM)
        } returns Optional.of(bot)
        every { chatRepository.saveAndFlush(any()) } throws
            constraintViolation(DatabaseConstraint.UQ_CHATS_MESSENGER_CHAT_ID_MESSENGER)

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.registerChat(
                IntegrationTestFixtures.BOT_ID,
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                Messenger.TELEGRAM,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                "Chat title",
                Language.EN.name,
                issuerHasMessengerChatAdminRights = true
            )
        }

        assertEquals(Status.ALREADY_EXISTS, exception.status)
        assertEquals(IntegrationResultKey.CHAT_ALREADY_REGISTERED.value(), exception.key)
    }

    @Test
    fun `deregisterChat deletes existing chat`() {
        val chat = IntegrationTestFixtures.chat(language = Language.RU)
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, true) } returns true
        every {
            chatRepository.findByMessengerChatIdAndMessenger(
                IntegrationTestFixtures.MESSENGER_CHAT_ID, Messenger.TELEGRAM
            )
        } returns chat
        every { chatRepository.delete(chat) } just runs

        val result = service.deregisterChat(
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Messenger.TELEGRAM,
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            issuerHasMessengerChatAdminRights = true
        )

        assertEquals(IntegrationResultKey.DEREGISTER_CHAT_SUCCESS.value(), result.key)
        assertEquals(Language.RU, result.language)
        verify { chatRepository.delete(chat) }
    }

    @Test
    fun `deregisterChat returns not registered when chat is missing`() {
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, true) } returns true
        every {
            chatRepository.findByMessengerChatIdAndMessenger(
                IntegrationTestFixtures.MESSENGER_CHAT_ID, Messenger.TELEGRAM
            )
        } returns null

        val result = service.deregisterChat(
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Messenger.TELEGRAM,
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            issuerHasMessengerChatAdminRights = true
        )

        assertEquals(IntegrationResultKey.DEREGISTER_CHAT_NOT_REGISTERED.value(), result.key)
        assertEquals(user.language, result.language)
        verify(exactly = 0) { chatRepository.delete(any()) }
    }

    @Test
    fun `deregisterChat fails when linked account is missing`() {
        everyAccountMissing()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.deregisterChat(
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM,
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                issuerHasMessengerChatAdminRights = true
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.LINKED_ACCOUNT_NOT_FOUND.value(), exception.key)
    }

    @Test
    fun `deregisterChat fails when permission is denied`() {
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, false) } returns false

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.deregisterChat(
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM,
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                issuerHasMessengerChatAdminRights = false
            )
        }

        assertEquals(Status.PERMISSION_DENIED, exception.status)
        assertEquals(IntegrationResultKey.CHAT_MANAGEMENT_DENIED.value(), exception.key)
    }

    @Test
    fun `updateChatInfo updates title and language`() {
        val chat = IntegrationTestFixtures.chat(language = Language.EN, title = "Old")
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, true) } returns true
        every {
            chatRepository.findByMessengerChatIdAndMessenger(
                IntegrationTestFixtures.MESSENGER_CHAT_ID, Messenger.TELEGRAM
            )
        } returns chat
        every { chatRepository.save(chat) } returns chat

        val result = service.updateChatInfo(
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            Messenger.TELEGRAM,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Language.RU,
            "New",
            issuerHasMessengerChatAdminRights = true
        )

        assertEquals(IntegrationResultKey.CHAT_INFO_UPDATE_SUCCESS.value(), result.key)
        assertEquals(Language.RU, chat.language)
        assertEquals("New", chat.title)
    }

    @Test
    fun `updateChatInfo keeps existing fields when title and language are null`() {
        val chat = IntegrationTestFixtures.chat(language = Language.EN, title = "Old")
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, true) } returns true
        every {
            chatRepository.findByMessengerChatIdAndMessenger(
                IntegrationTestFixtures.MESSENGER_CHAT_ID, Messenger.TELEGRAM
            )
        } returns chat
        every { chatRepository.save(chat) } returns chat

        service.updateChatInfo(
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            Messenger.TELEGRAM,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            null,
            null,
            issuerHasMessengerChatAdminRights = true
        )

        assertEquals(Language.EN, chat.language)
        assertEquals("Old", chat.title)
    }

    @Test
    fun `updateChatInfo fails when chat is missing`() {
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, true) } returns true
        every {
            chatRepository.findByMessengerChatIdAndMessenger(
                IntegrationTestFixtures.MESSENGER_CHAT_ID, Messenger.TELEGRAM
            )
        } returns null

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.updateChatInfo(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                Messenger.TELEGRAM,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Language.RU,
                "New",
                issuerHasMessengerChatAdminRights = true
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.CHAT_NOT_FOUND.value(), exception.key)
    }

    @Test
    fun `updateChatInfo fails when linked account is missing`() {
        everyAccountMissing()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.updateChatInfo(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                Messenger.TELEGRAM,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Language.RU,
                "New",
                issuerHasMessengerChatAdminRights = true
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.LINKED_ACCOUNT_NOT_FOUND.value(), exception.key)
    }

    @Test
    fun `updateChatInfo fails when permission is denied`() {
        everyAccountExists()
        every {
            permissionService.canManageIntegrationChat(user, false)
        } returns false

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.updateChatInfo(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                Messenger.TELEGRAM,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Language.RU,
                "New",
                issuerHasMessengerChatAdminRights = false
            )
        }

        assertEquals(Status.PERMISSION_DENIED, exception.status)
        assertEquals(IntegrationResultKey.CHAT_MANAGEMENT_DENIED.value(), exception.key)
    }

    private fun everyAccountExists() {
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(
                any(), IntegrationTestFixtures.ISSUER_ACCOUNT_ID
            )
        } returns Optional.of(account)
    }

    private fun everyAccountMissing() {
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(
                any(), IntegrationTestFixtures.ISSUER_ACCOUNT_ID
            )
        } returns Optional.empty()
    }
}
