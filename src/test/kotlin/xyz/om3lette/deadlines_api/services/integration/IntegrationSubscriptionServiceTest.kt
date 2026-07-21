package xyz.om3lette.deadlines_api.services.integration

import io.grpc.Status
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.db.constraintViolation
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.chat.model.ChatSubscription
import xyz.om3lette.deadlines_api.data.integration.chat.repo.ChatRepository
import xyz.om3lette.deadlines_api.data.integration.chat.repo.ChatSubscriptionRepository
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntegrationSubscriptionServiceTest {
    private val userMessengerAccountRepository: UserMessengerAccountRepository = mockk()
    private val permissionService: PermissionService = mockk()
    // Serves as an extraction of common require clauses and maps directly to permission service
    private val integrationPermissionValidator: IntegrationPermissionValidator = IntegrationPermissionValidator(
        permissionService
    )
    private val organizationRepository: OrganizationRepository = mockk()
    private val chatRepository: ChatRepository = mockk()
    private val chatSubscriptionRepository: ChatSubscriptionRepository = mockk()
    private val deadlineRepository: DeadlineRepository = mockk()
    private val threadRepository: ThreadRepository = mockk()
    private val languageResolver = IntegrationLanguageResolver(
        userMessengerAccountRepository,
        IntegrationTestFixtures.integrationProperties(fallbackLanguage = Language.RU)
    )
    private val service = IntegrationSubscriptionService(
        userMessengerAccountRepository,
        permissionService,
        integrationPermissionValidator,
        organizationRepository,
        chatRepository,
        chatSubscriptionRepository,
        deadlineRepository,
        threadRepository,
        languageResolver
    )

    private val user = IntegrationTestFixtures.user(language = Language.EN)
    private val account = IntegrationTestFixtures.messengerAccount(user = user)
    private val chat = IntegrationTestFixtures.chat(language = Language.RU)
    private val organization = DomainObjectBuilder.organization(id = 101)
    private val thread = DomainObjectBuilder.thread(organization, id = 202)
    private val deadline = DomainObjectBuilder.deadline(thread, id = 303)

    @Test
    fun `subscribeToOrganization saves organization subscription`() {
        val savedSubscription = slot<ChatSubscription>()
        everyAccountExists()
        every { organizationRepository.findById(organization.id) } returns Optional.of(organization)
        every { permissionService.hasAccess(user, any<OrganizationScope>()) } returns true
        everyChatExists()
        every {
            chatSubscriptionRepository.saveAndFlush(capture(savedSubscription))
        } answers { savedSubscription.captured }

        val result = service.subscribeToOrganization(
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            organization.id,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Messenger.TELEGRAM
        )

        assertEquals(IntegrationResultKey.SUBSCRIBE_SUCCESS.value(ScopeType.ORGANIZATION), result.key)
        assertEquals(chat.language, result.language)
        assertEquals(organization.id, savedSubscription.captured.scopeId)
        assertEquals(ScopeType.ORGANIZATION, savedSubscription.captured.scopeType)
    }

    @Test
    fun `subscribeToThread saves thread subscription`() {
        val savedSubscription = slot<ChatSubscription>()
        everyAccountExists()
        every { threadRepository.findById(thread.id) } returns Optional.of(thread)
        every { permissionService.hasAccess(user, any<ThreadScope>()) } returns true
        everyChatExists()
        every { chatSubscriptionRepository.saveAndFlush(capture(savedSubscription)) } answers { savedSubscription.captured }

        val result = service.subscribeToThread(
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            thread.id,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Messenger.TELEGRAM
        )

        assertEquals(IntegrationResultKey.SUBSCRIBE_SUCCESS.value(ScopeType.THREAD), result.key)
        assertEquals(thread.id, savedSubscription.captured.scopeId)
        assertEquals(ScopeType.THREAD, savedSubscription.captured.scopeType)
    }

    @Test
    fun `subscribeToDeadline saves deadline subscription`() {
        val savedSubscription = slot<ChatSubscription>()
        everyAccountExists()
        every { deadlineRepository.findById(deadline.id) } returns Optional.of(deadline)
        every { permissionService.hasAccess(user, any<DeadlineScope>()) } returns true
        everyChatExists()
        every { chatSubscriptionRepository.saveAndFlush(capture(savedSubscription)) } answers { savedSubscription.captured }

        val result = service.subscribeToDeadline(
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            deadline.id,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Messenger.TELEGRAM
        )

        assertEquals(IntegrationResultKey.SUBSCRIBE_SUCCESS.value(ScopeType.DEADLINE), result.key)
        assertEquals(deadline.id, savedSubscription.captured.scopeId)
        assertEquals(ScopeType.DEADLINE, savedSubscription.captured.scopeType)
    }

    @Test
    fun `subscribe fails when issuer account is missing`() {
        everyAccountMissing()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToOrganization(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                organization.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.USER_NOT_FOUND.value(), exception.key)
        assertEquals(Language.RU, exception.language)
    }

    @Test
    fun `subscribe fails when issuer is not a chat administrator`() {
        everyAccountExists()
        every { permissionService.canManageIntegrationChat(user, false) } returns false

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToOrganization(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                organization.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.PERMISSION_DENIED, exception.status)
        assertEquals(IntegrationResultKey.CHAT_MANAGEMENT_DENIED.value(), exception.key)
    }

    @Test
    fun `subscribeToOrganization fails when organization is missing`() {
        everyAccountExists()
        every { organizationRepository.findById(organization.id) } returns Optional.empty()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToOrganization(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                organization.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.ORGANIZATION_NOT_FOUND.value(), exception.key)
    }

    @Test
    fun `subscribeToOrganization fails when permission is denied`() {
        everyAccountExists()
        every { organizationRepository.findById(organization.id) } returns Optional.of(organization)
        every { permissionService.hasAccess(user, any<OrganizationScope>()) } returns false

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToOrganization(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                organization.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.PERMISSION_DENIED, exception.status)
        assertEquals(IntegrationResultKey.ORGANIZATION_ACCESS_DENIED.value(), exception.key)
    }

    @Test
    fun `subscribeToThread fails when thread is missing`() {
        everyAccountExists()
        every { threadRepository.findById(thread.id) } returns Optional.empty()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToThread(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                thread.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.THREAD_NOT_FOUND.value(), exception.key)
    }

    @Test
    fun `subscribeToThread fails when permission is denied`() {
        everyAccountExists()
        every { threadRepository.findById(thread.id) } returns Optional.of(thread)
        every { permissionService.hasAccess(user, any<ThreadScope>()) } returns false

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToThread(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                thread.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.PERMISSION_DENIED, exception.status)
        assertEquals(IntegrationResultKey.THREAD_ACCESS_DENIED.value(), exception.key)
    }

    @Test
    fun `subscribeToDeadline fails when chat is missing`() {
        everyAccountExists()
        every { deadlineRepository.findById(deadline.id) } returns Optional.of(deadline)
        every { permissionService.hasAccess(user, any<DeadlineScope>()) } returns true
        every {
            chatRepository.findByMessengerChatIdAndMessenger(
                IntegrationTestFixtures.MESSENGER_CHAT_ID, Messenger.TELEGRAM
            )
        } returns null

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToDeadline(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                deadline.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.CHAT_NOT_FOUND.value(), exception.key)
    }

    @Test
    fun `subscribeToDeadline fails when deadline is missing`() {
        everyAccountExists()
        every { deadlineRepository.findById(deadline.id) } returns Optional.empty()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToDeadline(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                deadline.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.DEADLINE_NOT_FOUND.value(), exception.key)
    }

    @Test
    fun `subscribeToDeadline fails when permission is denied`() {
        everyAccountExists()
        every { deadlineRepository.findById(deadline.id) } returns Optional.of(deadline)
        every { permissionService.hasAccess(user, any<DeadlineScope>()) } returns false

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToDeadline(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                deadline.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.PERMISSION_DENIED, exception.status)
        assertEquals(IntegrationResultKey.DEADLINE_ACCESS_DENIED.value(), exception.key)
    }

    @Test
    fun `subscribe fails when subscription already exists`() {
        everyAccountExists()
        every { organizationRepository.findById(organization.id) } returns Optional.of(organization)
        every { permissionService.hasAccess(user, any<OrganizationScope>()) } returns true
        everyChatExists()
        every { chatSubscriptionRepository.saveAndFlush(any()) } throws
            constraintViolation(DatabaseConstraint.PK_CHAT_SUBSCRIPTIONS)

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.subscribeToOrganization(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                organization.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.ALREADY_EXISTS, exception.status)
        assertEquals(IntegrationResultKey.SUBSCRIBE_ALREADY_SUBSCRIBED.value(ScopeType.ORGANIZATION), exception.key)
        assertEquals(chat.language, exception.language)
    }

    @Test
    fun `unsubscribeFromOrganization deletes exact organization and child subscriptions`() {
        everyAccountExists()
        everyChatExists()
        every { chatSubscriptionRepository.deleteByChatAndScopeIdAndScopeType(chat, organization.id, ScopeType.ORGANIZATION) } returns 1
        every { threadRepository.findAllIdsByOrganizationId(organization.id) } returns listOf(202, 203)
        every { deadlineRepository.findAllIdsByOrganizationId(organization.id) } returns listOf(303)
        every {
            chatSubscriptionRepository.deleteAllByChatAndScopeTypeAndScopeIdIn(chat, ScopeType.THREAD, listOf(202, 203))
        } returns 2
        every {
            chatSubscriptionRepository.deleteAllByChatAndScopeTypeAndScopeIdIn(chat, ScopeType.DEADLINE, listOf(303))
        } returns 1

        val result = service.unsubscribeFromOrganization(
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            organization.id,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Messenger.TELEGRAM
        )

        assertEquals(IntegrationResultKey.UNSUBSCRIBE_SUCCESS.value(ScopeType.ORGANIZATION), result.key)
        assertEquals(chat.language, result.language)
    }

    @Test
    fun `unsubscribeFromThread deletes exact thread and child deadline subscriptions`() {
        everyAccountExists()
        everyChatExists()
        every { chatSubscriptionRepository.deleteByChatAndScopeIdAndScopeType(chat, thread.id, ScopeType.THREAD) } returns 1
        every { deadlineRepository.findAllIdsByThreadId(thread.id) } returns listOf(303, 304)
        every {
            chatSubscriptionRepository.deleteAllByChatAndScopeTypeAndScopeIdIn(chat, ScopeType.DEADLINE, listOf(303, 304))
        } returns 2

        val result = service.unsubscribeFromThread(
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            thread.id,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Messenger.TELEGRAM
        )

        assertEquals(IntegrationResultKey.UNSUBSCRIBE_SUCCESS.value(ScopeType.THREAD), result.key)
    }

    @Test
    fun `unsubscribeFromDeadline returns not subscribed when nothing is deleted`() {
        everyAccountExists()
        everyChatExists()
        every { chatSubscriptionRepository.deleteByChatAndScopeIdAndScopeType(chat, deadline.id, ScopeType.DEADLINE) } returns 0

        val result = service.unsubscribeFromDeadline(
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            deadline.id,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Messenger.TELEGRAM
        )

        assertEquals(IntegrationResultKey.UNSUBSCRIBE_NOT_SUBSCRIBED.value(ScopeType.DEADLINE), result.key)
    }

    @Test
    fun `unsubscribe fails when chat is missing`() {
        every { chatRepository.findByMessengerChatIdAndMessenger(
            IntegrationTestFixtures.MESSENGER_CHAT_ID, Messenger.TELEGRAM
        ) } returns null
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(
                Messenger.TELEGRAM, IntegrationTestFixtures.ISSUER_ACCOUNT_ID
            )
        } returns Optional.of(account)
        every { permissionService.canManageIntegrationChat(user, false) } returns true

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.unsubscribeFromDeadline(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                deadline.id,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.CHAT_NOT_FOUND.value(), exception.key)
    }

    @Test
    fun `unsubscribeFromAll deletes all chat subscriptions`() {
        everyAccountExists()
        everyChatExists()
        every { chatSubscriptionRepository.deleteAllByChat(chat) } returns 3

        val result = service.unsubscribeFromAll(
            IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
            IntegrationTestFixtures.MESSENGER_CHAT_ID,
            Messenger.TELEGRAM
        )

        assertEquals(IntegrationResultKey.UNSUBSCRIBE_ALL_SUCCESS.value(), result.key)
        assertEquals(chat.language, result.language)
    }

    @Test
    fun `unsubscribeFromAll fails when chat is missing`() {
        every {
            chatRepository.findByMessengerChatIdAndMessenger(
                IntegrationTestFixtures.MESSENGER_CHAT_ID, Messenger.TELEGRAM
            )
        } returns null
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(
                Messenger.TELEGRAM, IntegrationTestFixtures.ISSUER_ACCOUNT_ID
            )
        } returns Optional.of(account)
        every { permissionService.canManageIntegrationChat(user, false) } returns true

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.unsubscribeFromAll(
                IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                IntegrationTestFixtures.MESSENGER_CHAT_ID,
                Messenger.TELEGRAM
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.CHAT_NOT_FOUND.value(), exception.key)
    }

    private fun everyAccountExists() {
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(
                any(), IntegrationTestFixtures.ISSUER_ACCOUNT_ID
            )
        } returns Optional.of(account)
        every { permissionService.canManageIntegrationChat(user, false) } returns true
    }

    private fun everyAccountMissing() {
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(
                any(), IntegrationTestFixtures.ISSUER_ACCOUNT_ID
            )
        } returns Optional.empty()
    }

    private fun everyChatExists() {
        every {
            chatRepository.findByMessengerChatIdAndMessenger(
                IntegrationTestFixtures.MESSENGER_CHAT_ID, any()
            )
        } returns chat
    }
}
