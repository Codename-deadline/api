package xyz.om3lette.deadlines_api.db

import org.springframework.jdbc.core.JdbcTemplate
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.user.enums.UserRole

object DatabaseObjectBuilder {
    fun insertUser(
        jdbc: JdbcTemplate,
        id: Long,
        username: String,
        fullName: String = "Database Test User",
        language: Language = Language.EN,
        role: UserRole = UserRole.USER
    ) {
        jdbc.update(
            """
            INSERT INTO users (id, username, joined_at, full_name, language, last_password_change, role)
            VALUES (?, ?, now(), ?, ?, now(), ?)
            """.trimIndent(),
            id,
            username,
            fullName,
            language.name,
            role.name
        )
    }

    fun insertOrganization(
        jdbc: JdbcTemplate,
        id: Long,
        title: String = "Database Test Organization",
        type: OrganizationType = OrganizationType.PRIVATE
    ) {
        jdbc.update(
            """
            INSERT INTO organizations (id, title, type, created_at)
            VALUES (?, ?, ?, now())
            """.trimIndent(),
            id,
            title,
            type.name
        )
    }

    fun insertThread(
        jdbc: JdbcTemplate,
        id: Long,
        organizationId: Long,
        title: String = "Database Test Thread"
    ) {
        jdbc.update(
            """
            INSERT INTO threads (id, title, organization_id, created_at)
            VALUES (?, ?, ?, now())
            """.trimIndent(),
            id,
            title,
            organizationId
        )
    }

    fun insertDeadline(
        jdbc: JdbcTemplate,
        id: Long,
        threadId: Long,
        title: String = "Database Test Deadline",
        isCompleted: Boolean = false
    ) {
        jdbc.update(
            """
            INSERT INTO deadlines (id, thread_id, title, created_at, due, is_completed)
            VALUES (?, ?, ?, now(), now() + interval '1 day', ?)
            """.trimIndent(),
            id,
            threadId,
            title,
            isCompleted
        )
    }

    fun insertBot(
        jdbc: JdbcTemplate,
        id: Long,
        botId: Long,
        username: String = "database_test_bot",
        messenger: Messenger = Messenger.TELEGRAM
    ) {
        jdbc.update(
            "INSERT INTO bots (id, messenger, bot_id, username) VALUES (?, ?, ?, ?)",
            id,
            messenger.name,
            botId,
            username
        )
    }

    fun insertChat(
        jdbc: JdbcTemplate,
        id: Long,
        messengerChatId: Long,
        botId: Long,
        title: String = "Database Test Chat",
        messenger: Messenger = Messenger.TELEGRAM,
        language: Language = Language.EN
    ) {
        jdbc.update(
            """
            INSERT INTO chats (id, messenger_chat_id, messenger, title, bot_id, language, registered_at)
            VALUES (?, ?, ?, ?, ?, ?, now())
            """.trimIndent(),
            id,
            messengerChatId,
            messenger.name,
            title,
            botId,
            language.name
        )
    }

    fun insertUserScope(
        jdbc: JdbcTemplate,
        userId: Long,
        scopeType: ScopeType,
        scopeId: Long,
        role: ScopeRole
    ) {
        jdbc.update(
            """
            INSERT INTO user_scopes (user_id, scope_type, scope_id, role, assigned_at)
            VALUES (?, ?, ?, ?, now())
            """.trimIndent(),
            userId,
            scopeType.code,
            scopeId,
            role.name
        )
    }

    fun insertChatSubscription(
        jdbc: JdbcTemplate,
        chatId: Long,
        scopeType: ScopeType,
        scopeId: Long
    ) {
        jdbc.update(
            """
            INSERT INTO chat_subscriptions (chat_id, scope_type, scope_id, subscribed_at)
            VALUES (?, ?, ?, now())
            """.trimIndent(),
            chatId,
            scopeType.code,
            scopeId
        )
    }

    fun insertOrganizationInvitation(
        jdbc: JdbcTemplate,
        id: Long,
        invitedByUserId: Long,
        invitedUserId: Long,
        organizationId: Long,
        status: InvitationStatus = InvitationStatus.PENDING,
        role: ScopeRole = ScopeRole.ORG_MEMBER
    ) {
        jdbc.update(
            """
            INSERT INTO organization_invitations (
                id, invited_by_user_id, invited_user_id, organization_id, status, role, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, now())
            """.trimIndent(),
            id,
            invitedByUserId,
            invitedUserId,
            organizationId,
            status.name,
            role.name
        )
    }
}
