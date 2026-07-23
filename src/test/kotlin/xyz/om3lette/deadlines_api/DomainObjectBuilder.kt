package xyz.om3lette.deadlines_api

import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationInvitation
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.jwt.model.RefreshToken
import xyz.om3lette.deadlines_api.data.user.enums.UserRole
import xyz.om3lette.deadlines_api.data.user.model.User
import java.time.Instant
import java.time.temporal.ChronoUnit

object DomainObjectBuilder {
    fun organization(
        id: Long = 42,
        title: String = "test-org",
        description: String? = "test-org-desc",
        type: OrganizationType = OrganizationType.PUBLIC,
        createdAt: Instant = Instant.now()
    ): Organization =
        Organization(
            id = id,
            title = title,
            description = description,
            type = type,
            createdAt = createdAt
        )

    fun thread(
        org: Organization,
        id: Long = 52,
        title: String = "test-thread",
        description: String? = "test-thread-desc",
        createdAt: Instant = Instant.now()
    ): Thread =
        Thread(
            id = id,
            title = title,
            description = description,
            createdAt = createdAt,
            organization = org
        )

    fun deadline(
        thread: Thread,
        id: Long = 62,
        title: String = "test-deadline",
        description: String? = "test-deadline-desc",
        createdAt: Instant = Instant.now(),
        due: Instant = Instant.now().plus(5, ChronoUnit.MINUTES),
        isCompleted: Boolean = false
    ): Deadline =
        Deadline(
            id = id,
            thread = thread,
            isCompleted = isCompleted,
            title = title,
            description = description,
            createdAt = createdAt,
            due = due,
        )

    fun user(
        id: Long = 1,
        username: String = "test-user",
        fullName: String = "Test User",
        password: String? = null,
        language: Language = Language.EN,
        timeZone: String = "Etc/UTC",
        role: UserRole = UserRole.USER,
        joinedAt: Instant = Instant.now(),
        lastPasswordChange: Instant = Instant.EPOCH
    ): User =
        User(
            id = id,
            _username = username,
            joinedAt = joinedAt,
            fullName = fullName,
            _password = password,
            language = language,
            timeZone = timeZone,
            lastPasswordChange = lastPasswordChange,
            role = role
        )

    fun admin(): User = user(
        id = 0,
        username = "Admin",
        fullName = "Administrator",
        role = UserRole.ADMIN
    )

    fun userBob(): User = user(
        id = 1,
        username = "bob-the-tester",
        fullName = "Bob the tester"
    )

    fun userAlice(): User = user(
        id = 2,
        username = "alice-the-tester",
        fullName = "Alice the tester"
    )

    fun userScope(
        user: User,
        scopeType: ScopeType,
        scopeId: Long,
        role: ScopeRole,
        assignedAt: Instant = Instant.now()
    ): UserScope =
        UserScope(
            user = user,
            scopeType = scopeType,
            scopeId = scopeId,
            role = role,
            assignedAt = assignedAt
        )

    fun organizationInvitation(
        invitedBy: User,
        invitedUser: User,
        organization: Organization,
        role: ScopeRole = ScopeRole.ORG_MEMBER,
        status: InvitationStatus = InvitationStatus.PENDING,
        id: Long = 82,
        createdAt: Instant = Instant.now(),
        answeredAt: Instant? = null
    ): OrganizationInvitation =
        OrganizationInvitation(
            id = id,
            invitedBy = invitedBy,
            invitedUser = invitedUser,
            organization = organization,
            status = status,
            role = role,
            createdAt = createdAt,
            answeredAt = answeredAt
        )

    fun refreshToken(
        user: User,
        id: Long = 92,
        jti: String = "test-jti-$id",
        expiry: Instant = Instant.now().plusSeconds(60),
        revoked: Boolean = false
    ): RefreshToken =
        RefreshToken(
            id = id,
            jti = jti,
            expiry = expiry,
            revoked = revoked,
            user = user
        )
}
