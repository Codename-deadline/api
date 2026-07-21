package xyz.om3lette.deadlines_api.db

import io.grpc.Status
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.config.TestInfraMocks
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.chat.model.ChatSubscription
import xyz.om3lette.deadlines_api.data.integration.chat.model.ChatSubscriptionId
import xyz.om3lette.deadlines_api.data.integration.chat.repo.ChatSubscriptionRepository
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScopeId
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertBot
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertChat
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertChatSubscription
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertDeadline
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertOrganization
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertOrganizationInvitation
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertThread
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertUser
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertUserMessengerAccount
import xyz.om3lette.deadlines_api.db.DatabaseObjectBuilder.insertUserScope
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import xyz.om3lette.deadlines_api.services.auth.otp.UserRegistrationService
import xyz.om3lette.deadlines_api.services.integration.IntegrationChatService
import xyz.om3lette.deadlines_api.util.jpaRepository.violatesConstraint
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Tag("testcontainers")
@ActiveProfiles("test")
@Import(TestInfraMocks::class, TestDatabaseConfig::class)
class DatabaseIntegrationTests {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var threadRepository: ThreadRepository

    @Autowired
    private lateinit var deadlineRepository: DeadlineRepository

    @Autowired
    private lateinit var userScopeRepository: UserScopeRepository

    @Autowired
    private lateinit var chatSubscriptionRepository: ChatSubscriptionRepository

    @Autowired
    private lateinit var userRegistrationService: UserRegistrationService

    @Autowired
    private lateinit var integrationChatService: IntegrationChatService

    @Test
    @Transactional
    fun `scope target trigger rejects missing targets`() {
        insertUser(jdbc, 1001, "missing-target")

        assertThrows<DataIntegrityViolationException> {
            insertUserScope(jdbc, 1001, ScopeType.ORGANIZATION, 9999, ScopeRole.ORG_MEMBER)
        }
    }

    @Test
    @Transactional
    fun `scope constraints reject incompatible roles`() {
        insertUser(jdbc, 1002, "first-owner")
        insertOrganization(jdbc, 2001)

        assertThrows<DataIntegrityViolationException> {
            insertUserScope(jdbc, 1002, ScopeType.ORGANIZATION, 2001, ScopeRole.THR_ADMIN)
        }
    }

    @Test
    @Transactional
    fun `username uniqueness is case insensitive`() {
        val username = "CaseSensitiveName"
        insertUser(jdbc, 1006, username)

        assertThrows<DataIntegrityViolationException> {
            insertUser(jdbc, 1007, username.lowercase())
        }
    }

    @Test
    @Transactional
    fun `pending invitation index prevents duplicates`() {
        insertUser(jdbc, 1008, "invitation-sender")
        insertUser(jdbc, 1009, "invitation-recipient")
        insertOrganization(jdbc, 2004)
        insertOrganizationInvitation(jdbc, 5001, 1008, 1009, 2004)

        assertThrows<DataIntegrityViolationException> {
            insertOrganizationInvitation(
                jdbc,
                id = 5002,
                invitedByUserId = 1008,
                invitedUserId = 1009,
                organizationId = 2004,
                role = ScopeRole.ORG_ADMIN
            )
        }
    }

    @Test
    @Transactional
    fun `organization owner index prevents concurrent duplicate owners`() {
        insertUser(jdbc, 1003, "first-owner-index")
        insertUser(jdbc, 1004, "second-owner-index")
        insertOrganization(jdbc, 2002)
        insertUserScope(jdbc, 1003, ScopeType.ORGANIZATION, 2002, ScopeRole.ORG_OWNER)

        assertThrows<DataIntegrityViolationException> {
            insertUserScope(jdbc, 1004, ScopeType.ORGANIZATION, 2002, ScopeRole.ORG_OWNER)
        }
    }

    @Test
    @Transactional
    fun `deleting scope target cleans polymorphic associations`() {
        insertUser(jdbc, 1005, "cleanup-owner")
        insertOrganization(jdbc, 2003)
        insertBot(jdbc, 3001, 4001, "cleanup_bot")
        insertChat(jdbc, 3002, 4002, 3001, "Cleanup chat")
        insertUserScope(jdbc, 1005, ScopeType.ORGANIZATION, 2003, ScopeRole.ORG_OWNER)
        insertChatSubscription(jdbc, 3002, ScopeType.ORGANIZATION, 2003)

        jdbc.update("DELETE FROM organizations WHERE id = 2003")

        assertEquals(
            0,
            jdbc.queryForObject("SELECT count(*) FROM user_scopes WHERE scope_id = 2003", Int::class.java)
        )
        assertEquals(
            0,
            jdbc.queryForObject("SELECT count(*) FROM chat_subscriptions WHERE scope_id = 2003", Int::class.java)
        )
    }

    @Test
    @Transactional
    fun `JPA composite scope identifiers persist compact scope codes`() {
        insertUser(jdbc, 1010, "scope-code-user")
        insertOrganization(jdbc, 2005)
        insertBot(jdbc, 3003, 4003, "scope_code_bot")
        insertChat(jdbc, 3004, 4004, 3003, "Scope code chat")

        val user = entityManager.find(User::class.java, 1010L)
        val chat = entityManager.find(Chat::class.java, 3004L)
        entityManager.persist(
            UserScope(user, ScopeType.ORGANIZATION, 2005, ScopeRole.ORG_MEMBER, Instant.now())
        )
        entityManager.persist(
            ChatSubscription(chat, 2005, ScopeType.ORGANIZATION, Instant.now())
        )
        entityManager.flush()
        entityManager.clear()

        assertEquals(
            ScopeType.ORGANIZATION.code,
            jdbc.queryForObject(
                "SELECT scope_type FROM user_scopes WHERE user_id = 1010 AND scope_id = 2005",
                String::class.java
            )
        )
        assertEquals(
            ScopeType.ORGANIZATION.code,
            jdbc.queryForObject(
                "SELECT scope_type FROM chat_subscriptions WHERE chat_id = 3004 AND scope_id = 2005",
                String::class.java
            )
        )
        val loadedUserScope = entityManager.find(
            UserScope::class.java,
            UserScopeId(1010, ScopeType.ORGANIZATION, 2005)
        )
        val loadedSubscription = entityManager.find(
            ChatSubscription::class.java,
            ChatSubscriptionId(3004, 2005, ScopeType.ORGANIZATION)
        )
        assertEquals(ScopeType.ORGANIZATION, loadedUserScope.scopeType)
        assertEquals(ScopeType.ORGANIZATION, loadedSubscription.scopeType)
    }

    @Test
    @Transactional
    fun `HQL scope string literals match compact persisted codes`() {
        insertUser(jdbc, 1011, "hql-scope-user")
        insertOrganization(jdbc, 2006)
        insertThread(jdbc, 2101, 2006)
        insertDeadline(jdbc, 2201, 2101)
        insertUserScope(jdbc, 1011, ScopeType.ORGANIZATION, 2006, ScopeRole.ORG_MEMBER)
        insertUserScope(jdbc, 1011, ScopeType.THREAD, 2101, ScopeRole.THR_ASSIGNEE)
        insertUserScope(jdbc, 1011, ScopeType.DEADLINE, 2201, ScopeRole.DDL_ASSIGNEE)
        val user = entityManager.find(User::class.java, 1011L)

        assertEquals(
            listOf(2006L),
            organizationRepository.findAllOrganizationsForUser(user, Pageable.ofSize(10)).content.map { it.id }
        )
        assertEquals(
            listOf(2101L),
            threadRepository.findAllByUser(1011, Pageable.ofSize(10)).content.map { it.id }
        )
        assertEquals(
            listOf(2201L),
            deadlineRepository.findAllByUser(1011, Pageable.ofSize(10)).content.map { it.id }
        )
        assertEquals(
            setOf(ScopeType.ORGANIZATION, ScopeType.THREAD, ScopeType.DEADLINE),
            userScopeRepository.findUserRolesInScope(1011, 2006, 2101, 2201).map { it.scopeType }.toSet()
        )
    }

    @Test
    @Transactional
    fun `new composite user scope uses insert semantics`() {
        insertUser(jdbc, 1012, "duplicate-scope-user")
        insertOrganization(jdbc, 2007)
        insertUserScope(jdbc, 1012, ScopeType.ORGANIZATION, 2007, ScopeRole.ORG_MEMBER)
        entityManager.clear()

        val error = assertThrows<DataIntegrityViolationException> {
            userScopeRepository.saveAndFlush(
                UserScope(
                    entityManager.find(User::class.java, 1012L),
                    ScopeType.ORGANIZATION,
                    2007,
                    ScopeRole.ORG_ADMIN,
                    Instant.now()
                )
            )
        }

        assertTrue(error.violatesConstraint(DatabaseConstraint.PK_USER_SCOPES))
    }

    @Test
    @Transactional
    fun `new composite chat subscription uses insert semantics`() {
        insertOrganization(jdbc, 2008)
        insertBot(jdbc, 3005, 4005, "duplicate_subscription_bot")
        insertChat(jdbc, 3006, 4006, 3005, "Duplicate subscription chat")
        insertChatSubscription(jdbc, 3006, ScopeType.ORGANIZATION, 2008)
        entityManager.clear()

        val error = assertThrows<DataIntegrityViolationException> {
            chatSubscriptionRepository.saveAndFlush(
                ChatSubscription(
                    entityManager.find(Chat::class.java, 3006L),
                    2008,
                    ScopeType.ORGANIZATION,
                    Instant.now()
                )
            )
        }

        assertTrue(error.violatesConstraint(DatabaseConstraint.PK_CHAT_SUBSCRIPTIONS))
    }

    @Test
    fun `failed messenger account registration rolls back the new user`() {
        insertUser(jdbc, 1013, "existing-messenger-user")
        insertUserMessengerAccount(jdbc, 3007, 1013, 7777)

        val error = assertThrows<StatusCodeException> {
            userRegistrationService.registerExternalUser(
                "rolled-back-user",
                "Rolled Back User",
                OtpChannel.TELEGRAM,
                Language.EN,
                "7777"
            )
        }

        assertEquals(ErrorCode.INTEGRATION_ACCOUNT_ALREADY_IN_USE, error.code)
        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE username = 'rolled-back-user'",
                Int::class.java
            )
        )
    }

    @Test
    fun `duplicate chat is translated inside the service transaction`() {
        insertUser(jdbc, 1014, "duplicate-chat-user")
        insertUserMessengerAccount(jdbc, 3008, 1014, 8888)
        insertBot(jdbc, 3009, 4009, "duplicate_chat_bot")
        insertChat(jdbc, 3010, -1001630629206, 3009, "Existing chat")

        val error = assertThrows<GrpcKeyLocaleException> {
            integrationChatService.registerChat(
                4009,
                8888,
                Messenger.TELEGRAM,
                -1001630629206,
                "Duplicate chat",
                Language.EN.name,
                issuerHasMessengerChatAdminRights = true
            )
        }

        assertEquals(Status.ALREADY_EXISTS, error.status)
        assertEquals(IntegrationResultKey.CHAT_ALREADY_REGISTERED.value(), error.key)
    }
}
