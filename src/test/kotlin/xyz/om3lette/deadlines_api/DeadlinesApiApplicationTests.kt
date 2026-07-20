package xyz.om3lette.deadlines_api

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import xyz.om3lette.deadlines_api.config.TestInfraMocks
import xyz.om3lette.deadlines_api.data.common.validation.MinimumValidationReason
import xyz.om3lette.deadlines_api.data.common.validation.SimpleValidationReason
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.chat.model.ChatSubscription
import xyz.om3lette.deadlines_api.data.integration.chat.model.ChatSubscriptionId
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScopeId
import xyz.om3lette.deadlines_api.data.user.model.User
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Import(TestInfraMocks::class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeadlinesApiApplicationTests {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.4-alpine")
    }

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

	@Test
	fun contextLoads() {
	}

    @Test
    @Transactional
    fun `scope target trigger rejects missing targets`() {
        insertUser(1001, "missing-target")

        assertThrows<DataIntegrityViolationException> {
            jdbc.update(
                """
                INSERT INTO user_scopes (user_id, scope_type, scope_id, role, assigned_at)
                VALUES (1001, 'ORG', 9999, 'ORG_MEMBER', now())
                """.trimIndent()
            )
        }
    }

    @Test
    @Transactional
    fun `scope constraints reject incompatible roles`() {
        insertUser(1002, "first-owner")
        insertOrganization(2001)

        assertThrows<DataIntegrityViolationException> {
            jdbc.update(
                """
                INSERT INTO user_scopes (user_id, scope_type, scope_id, role, assigned_at)
                VALUES (1002, 'ORG', 2001, 'THR_ADMIN', now())
                """.trimIndent()
            )
        }
    }

    @Test
    @Transactional
    fun `username uniqueness is case insensitive`() {
        val username = "CaseSensitiveName"
        insertUser(1006, username)

        assertThrows<DataIntegrityViolationException> {
            insertUser(1007, username.lowercase())
        }
    }

    @Test
    @Transactional
    fun `pending invitation index prevents duplicates`() {
        insertUser(1008, "invitation-sender")
        insertUser(1009, "invitation-recipient")
        insertOrganization(2004)
        jdbc.update(
            """
            INSERT INTO organization_invitations (
                id, invited_by_user_id, invited_user_id, organization_id, status, role, created_at
            ) VALUES (5001, 1008, 1009, 2004, 'PENDING', 'ORG_MEMBER', now())
            """.trimIndent()
        )

        assertThrows<DataIntegrityViolationException> {
            jdbc.update(
                """
                INSERT INTO organization_invitations (
                    id, invited_by_user_id, invited_user_id, organization_id, status, role, created_at
                ) VALUES (5002, 1008, 1009, 2004, 'PENDING', 'ORG_ADMIN', now())
                """.trimIndent()
            )
        }
    }

    @Test
    @Transactional
    fun `organization owner index prevents concurrent duplicate owners`() {
        insertUser(1003, "first-owner-index")
        insertUser(1004, "second-owner-index")
        insertOrganization(2002)
        jdbc.update(
            """
            INSERT INTO user_scopes (user_id, scope_type, scope_id, role, assigned_at)
            VALUES (1003, 'ORG', 2002, 'ORG_OWNER', now())
            """.trimIndent()
        )

        assertThrows<DataIntegrityViolationException> {
            jdbc.update(
                """
                INSERT INTO user_scopes (user_id, scope_type, scope_id, role, assigned_at)
                VALUES (1004, 'ORG', 2002, 'ORG_OWNER', now())
                """.trimIndent()
            )
        }
    }

    @Test
    @Transactional
    fun `deleting scope target cleans polymorphic associations`() {
        insertUser(1005, "cleanup-owner")
        insertOrganization(2003)
        jdbc.update("INSERT INTO bots (id, messenger, bot_id, username) VALUES (3001, 'TELEGRAM', 4001, 'cleanup_bot')")
        jdbc.update(
            """
            INSERT INTO chats (id, messenger_chat_id, messenger, title, bot_id, language, registered_at)
            VALUES (3002, 4002, 'TELEGRAM', 'Cleanup chat', 3001, 'EN', now())
            """.trimIndent()
        )
        jdbc.update(
            """
            INSERT INTO user_scopes (user_id, scope_type, scope_id, role, assigned_at)
            VALUES (1005, 'ORG', 2003, 'ORG_OWNER', now())
            """.trimIndent()
        )
        jdbc.update(
            """
            INSERT INTO chat_subscriptions (chat_id, scope_type, scope_id, subscribed_at)
            VALUES (3002, 'ORG', 2003, now())
            """.trimIndent()
        )

        jdbc.update("DELETE FROM organizations WHERE id = 2003")

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_scopes WHERE scope_id = 2003", Int::class.java))
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM chat_subscriptions WHERE scope_id = 2003", Int::class.java))
    }

    @Test
    @Transactional
    fun `JPA composite scope identifiers persist compact scope codes`() {
        insertUser(1010, "scope-code-user")
        insertOrganization(2005)
        jdbc.update("INSERT INTO bots (id, messenger, bot_id, username) VALUES (3003, 'TELEGRAM', 4003, 'scope_code_bot')")
        jdbc.update(
            """
            INSERT INTO chats (id, messenger_chat_id, messenger, title, bot_id, language, registered_at)
            VALUES (3004, 4004, 'TELEGRAM', 'Scope code chat', 3003, 'EN', now())
            """.trimIndent()
        )

        val user = entityManager.find(User::class.java, 1010L)
        val chat = entityManager.find(Chat::class.java, 3004L)
        entityManager.persist(
            UserScope(user, ScopeType.ORGANIZATION, 2005, ScopeRole.ORG_MEMBER, java.time.Instant.now())
        )
        entityManager.persist(
            ChatSubscription(chat, 2005, ScopeType.ORGANIZATION, java.time.Instant.now())
        )
        entityManager.flush()
        entityManager.clear()

        assertEquals(
            "ORG",
            jdbc.queryForObject(
                "SELECT scope_type FROM user_scopes WHERE user_id = 1010 AND scope_id = 2005",
                String::class.java
            )
        )
        assertEquals(
            "ORG",
            jdbc.queryForObject(
                "SELECT scope_type FROM chat_subscriptions WHERE chat_id = 3004 AND scope_id = 2005",
                String::class.java
            )
        )
        assertEquals(
            ScopeType.ORGANIZATION,
            entityManager.find(
                UserScope::class.java,
                UserScopeId(1010, ScopeType.ORGANIZATION, 2005)
            ).scopeType
        )
        assertEquals(
            ScopeType.ORGANIZATION,
            entityManager.find(
                ChatSubscription::class.java,
                ChatSubscriptionId(3004, 2005, ScopeType.ORGANIZATION)
            ).scopeType
        )
    }

    @Test
    fun validationResponseIsDocumentedInOpenApi() {
        MockMvcBuilders.webAppContextSetup(context).build()
            .get("/v3/api-docs")
            .andExpect {
                status { isOk() }
                jsonPath("$.paths['/api/user/hints'].get.responses['422'].content['application/json'].schema") {
                    exists()
                }
                jsonPath("$.components.schemas.ValidationViolation.discriminator.propertyName") {
                    value("reason")
                }
                jsonPath("$.components.schemas.ValidationViolation.oneOf") {
                    isArray()
                }
                jsonPath("$.components.schemas.SimpleValidationViolation.properties.reason.enum.length()") {
                    value(SimpleValidationReason.entries.size)
                }
                jsonPath("$.components.schemas.MinimumValidationViolation.properties.reason.enum.length()") {
                    value(MinimumValidationReason.entries.size)
                }
            }
    }

    private fun insertUser(id: Long, username: String) {
        jdbc.update(
            """
            INSERT INTO users (id, username, joined_at, full_name, language, last_password_change, role)
            VALUES (?, ?, now(), 'Database Test User', 'EN', now(), 'USER')
            """.trimIndent(),
            id,
            username
        )
    }

    private fun insertOrganization(id: Long) {
        jdbc.update(
            """
            INSERT INTO organizations (id, title, type, created_at)
            VALUES (?, 'Database Test Organization', 'PRIVATE', now())
            """.trimIndent(),
            id
        )
    }

}
