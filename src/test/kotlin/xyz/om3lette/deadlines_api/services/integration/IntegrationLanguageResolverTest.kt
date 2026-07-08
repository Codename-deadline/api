package xyz.om3lette.deadlines_api.services.integration

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import java.util.Optional
import kotlin.test.assertEquals

class IntegrationLanguageResolverTest {
    private val userMessengerAccountRepository: UserMessengerAccountRepository = mockk()
    private val resolver = IntegrationLanguageResolver(
        userMessengerAccountRepository,
        IntegrationTestFixtures.integrationProperties(fallbackLanguage = Language.RU)
    )

    @Test
    fun `resolve returns configured fallback when messenger is null`() {
        assertEquals(Language.RU, resolver.resolve(
            null, IntegrationTestFixtures.ISSUER_ACCOUNT_ID
        ))
    }

    @Test
    fun `resolve returns configured fallback when account id is null`() {
        assertEquals(Language.RU, resolver.resolve(Messenger.TELEGRAM, null))
    }

    @Test
    fun `resolve returns linked user language`() {
        val account = IntegrationTestFixtures.messengerAccount(
            user = IntegrationTestFixtures.user(language = Language.EN)
        )
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(Messenger.TELEGRAM, account.accountId)
        } returns Optional.of(account)

        assertEquals(Language.EN, resolver.resolve(Messenger.TELEGRAM, account.accountId))
    }

    @Test
    fun `resolve returns configured fallback when account is missing`() {
        every {
            userMessengerAccountRepository.findByMessengerAndAccountId(
                Messenger.TELEGRAM, IntegrationTestFixtures.ISSUER_ACCOUNT_ID
            )
        } returns Optional.empty()

        assertEquals(Language.RU, resolver.resolve(
            Messenger.TELEGRAM, IntegrationTestFixtures.ISSUER_ACCOUNT_ID
        ))
    }

    @Test
    fun `resolve with chat prefers chat language`() {
        val chat = IntegrationTestFixtures.chat(language = Language.EN)

        assertEquals(
            Language.EN,
            resolver.resolve(chat, Messenger.TELEGRAM, IntegrationTestFixtures.ISSUER_ACCOUNT_ID)
        )
    }
}
