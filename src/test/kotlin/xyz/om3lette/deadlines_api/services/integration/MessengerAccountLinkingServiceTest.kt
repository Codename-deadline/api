package xyz.om3lette.deadlines_api.services.integration

import io.grpc.Status
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.event.UserMessengerAccountLinkageEvent
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.model.AccountLinkageRequest
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.repo.AccountLinkageRepository
import xyz.om3lette.deadlines_api.services.integration.kafka.AccountLinkageProducer
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MessengerAccountLinkingServiceTest {
    private val userMessengerAccountRepository: UserMessengerAccountRepository = mockk()
    private val accountLinkageProducer: AccountLinkageProducer = mockk()
    private val accountLinkageRepository: AccountLinkageRepository = mockk()
    private val permissionService: PermissionService = mockk()
    private val userRepository: UserRepository = mockk()
    private val languageResolver = IntegrationLanguageResolver(
        userMessengerAccountRepository,
        IntegrationTestFixtures.integrationProperties(fallbackLanguage = Language.RU)
    )

    private val service = MessengerAccountLinkingService(
        userMessengerAccountRepository,
        accountLinkageProducer,
        accountLinkageRepository,
        permissionService,
        userRepository,
        languageResolver
    )

    private val user = IntegrationTestFixtures.user(language = Language.EN)

    @BeforeEach
    fun commonStubs() {
        every { accountLinkageRepository.delete(any<AccountLinkageRequest>()) } just runs
    }

    @Test
    fun `sendConfirmationForAccountLinkage saves request and sends producer event`() {
        val savedRequest = slot<AccountLinkageRequest>()
        val sentEvent = slot<UserMessengerAccountLinkageEvent>()
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(Messenger.TELEGRAM, IntegrationTestFixtures.ISSUER_ACCOUNT_ID)
        } returns Optional.empty()
        every { userMessengerAccountRepository.findAllByUserAndMessenger(user, Messenger.TELEGRAM) } returns emptyList()
        every { permissionService.canLinkAccount(user, 0) } returns true
        every { accountLinkageRepository.save(capture(savedRequest)) } answers { savedRequest.captured }
        every { accountLinkageProducer.sendToMessenger(Messenger.TELEGRAM, capture(sentEvent)) } returns
            CompletableFuture.completedFuture(mockk<SendResult<String, UserMessengerAccountLinkageEvent>>())

        service.sendConfirmationForAccountLinkage(user, IntegrationTestFixtures.ISSUER_ACCOUNT_ID, Messenger.TELEGRAM)

        assertEquals(IntegrationTestFixtures.ISSUER_ACCOUNT_ID, savedRequest.captured.accountId)
        assertEquals(Messenger.TELEGRAM, savedRequest.captured.messenger)
        assertEquals(user.id, savedRequest.captured.userId)
        assertEquals(savedRequest.captured.id, sentEvent.captured.requestId)
        assertEquals(IntegrationTestFixtures.ISSUER_ACCOUNT_ID, sentEvent.captured.accountId)
    }

    @Test
    fun `sendConfirmationForAccountLinkage fails when account is already linked`() {
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(Messenger.TELEGRAM, IntegrationTestFixtures.ISSUER_ACCOUNT_ID)
        } returns Optional.of(IntegrationTestFixtures.messengerAccount())

        val exception = assertFailsWith<StatusCodeException> {
            service.sendConfirmationForAccountLinkage(user, IntegrationTestFixtures.ISSUER_ACCOUNT_ID, Messenger.TELEGRAM)
        }

        assertEquals(403, exception.statusCode)
        assertEquals(ErrorCode.INTEGRATION_ACCOUNT_ALREADY_IN_USE, exception.code)
    }

    @Test
    fun `sendConfirmationForAccountLinkage fails when link limit is exceeded`() {
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(Messenger.TELEGRAM, IntegrationTestFixtures.ISSUER_ACCOUNT_ID)
        } returns Optional.empty()
        every { userMessengerAccountRepository.findAllByUserAndMessenger(user, Messenger.TELEGRAM) } returns listOf(
            IntegrationTestFixtures.messengerAccount(user = user)
        )
        every { permissionService.canLinkAccount(user, 1) } returns false

        val exception = assertFailsWith<StatusCodeException> {
            service.sendConfirmationForAccountLinkage(user, IntegrationTestFixtures.ISSUER_ACCOUNT_ID, Messenger.TELEGRAM)
        }

        assertEquals(409, exception.statusCode)
        assertEquals(ErrorCode.INTEGRATION_MESSENGER_LINKAGE_LIMIT_EXCEEDED, exception.code)
    }

    @Test
    fun `linkMessengerAccount accepts request and saves messenger account`() {
        val request = AccountLinkageRequest("request-id", IntegrationTestFixtures.ISSUER_ACCOUNT_ID, Messenger.TELEGRAM, user.id)
        val savedAccount = slot<UserMessengerAccount>()
        every { accountLinkageRepository.findById("request-id") } returns Optional.of(request)
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { userMessengerAccountRepository.save(capture(savedAccount)) } answers { savedAccount.captured }

        val result = service.linkMessengerAccount(
            "request-id",
            isAccepted = true,
            messengerAccountId = request.accountId,
            messenger = request.messenger
        )

        assertEquals(IntegrationResultKey.ACCOUNT_LINKAGE_SUCCESS.value(), result.key)
        assertEquals(user.language, result.language)
        assertEquals(user, savedAccount.captured.user)
        assertEquals(request.accountId, savedAccount.captured.accountId)
        assertEquals(request.messenger, savedAccount.captured.messenger)
        verify { accountLinkageRepository.delete(request) }
    }

    @Test
    fun `linkMessengerAccount declines request without saving messenger account`() {
        val request = AccountLinkageRequest("request-id", IntegrationTestFixtures.ISSUER_ACCOUNT_ID, Messenger.TELEGRAM, user.id)
        every { accountLinkageRepository.findById("request-id") } returns Optional.of(request)
        every { userRepository.findById(user.id) } returns Optional.of(user)

        val result = service.linkMessengerAccount(
            "request-id",
            isAccepted = false,
            messengerAccountId = request.accountId,
            messenger = request.messenger
        )

        assertEquals(IntegrationResultKey.ACCOUNT_LINKAGE_IGNORED.value(), result.key)
        assertEquals(user.language, result.language)
        verify { accountLinkageRepository.delete(request) }
        verify(exactly = 0) { userMessengerAccountRepository.save(any()) }
    }

    @Test
    fun `linkMessengerAccount declines request with fallback language when user is missing`() {
        val request = AccountLinkageRequest("request-id", IntegrationTestFixtures.ISSUER_ACCOUNT_ID, Messenger.TELEGRAM, 99)
        every { accountLinkageRepository.findById("request-id") } returns Optional.of(request)
        every { userRepository.findById(99) } returns Optional.empty()

        val result = service.linkMessengerAccount(
            "request-id",
            isAccepted = false,
            messengerAccountId = request.accountId,
            messenger = request.messenger
        )

        assertEquals(IntegrationResultKey.ACCOUNT_LINKAGE_IGNORED.value(), result.key)
        assertEquals(Language.RU, result.language)
    }

    @Test
    fun `linkMessengerAccount fails when request is missing`() {
        every { accountLinkageRepository.findById("missing") } returns Optional.empty()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.linkMessengerAccount(
                "missing",
                isAccepted = true,
                messengerAccountId = IntegrationTestFixtures.ISSUER_ACCOUNT_ID,
                messenger = Messenger.TELEGRAM
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.REQUEST_NOT_FOUND.value(), exception.key)
        assertEquals(Language.RU, exception.language)
    }

    @Test
    fun `linkMessengerAccount fails accepted request when user is missing`() {
        val request = AccountLinkageRequest("request-id", IntegrationTestFixtures.ISSUER_ACCOUNT_ID, Messenger.TELEGRAM, 99)
        every { accountLinkageRepository.findById("request-id") } returns Optional.of(request)
        every { userRepository.findById(99) } returns Optional.empty()

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.linkMessengerAccount(
                "request-id",
                isAccepted = true,
                messengerAccountId = request.accountId,
                messenger = request.messenger
            )
        }

        assertEquals(Status.NOT_FOUND, exception.status)
        assertEquals(IntegrationResultKey.USER_NOT_FOUND.value(), exception.key)
        assertEquals(Language.RU, exception.language)
        verify(exactly = 0) { accountLinkageRepository.delete(request) }
    }

    @Test
    fun `linkMessengerAccount rejects a confirmation from another messenger account`() {
        val request = AccountLinkageRequest("request-id", IntegrationTestFixtures.ISSUER_ACCOUNT_ID, Messenger.TELEGRAM, user.id)
        every { accountLinkageRepository.findById("request-id") } returns Optional.of(request)

        val exception = assertFailsWith<GrpcKeyLocaleException> {
            service.linkMessengerAccount(
                "request-id",
                isAccepted = true,
                messengerAccountId = 999,
                messenger = Messenger.TELEGRAM
            )
        }

        assertEquals(Status.PERMISSION_DENIED, exception.status)
        assertEquals(IntegrationResultKey.REQUEST_NOT_FOUND.value(), exception.key)
        verify(exactly = 0) { accountLinkageRepository.delete(request) }
    }
}
