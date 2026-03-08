package xyz.om3lette.deadlines_api

import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.enums.ProgressionStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.user.enums.UserRole
import xyz.om3lette.deadlines_api.data.user.model.User
import java.time.Instant
import java.time.temporal.ChronoUnit

object DomainObjectBuilder {
    fun organization(type: OrganizationType = OrganizationType.PUBLIC): Organization =
        Organization(
            id = (42 + type.ordinal).toLong(),
            title = "test-org",
            description = "test-org-desc",
            type = type,
            createdAt = Instant.now()
        )

    fun thread(org: Organization): Thread =
        Thread(
            id = 52,
            title = "test-thread",
            description = "test-thread-desc",
            createdAt = Instant.now(),
            organization = org
        )

    fun deadline(org: Organization, thread: Thread): Deadline =
        Deadline(
            id = 52,
            organization = org,
            thread = thread,
            progress = 50,
            status = ProgressionStatus.IN_PROGRESS,
            title = "test-deadline",
            description = "test-deadline-desc",
            createdAt = Instant.now(),
            due = Instant.now().plus(5, ChronoUnit.MINUTES),
        )

    fun admin(): User =
        User(
            id = 0,
            _username = "Admin",
            joinedAt = Instant.now(),
            fullName = "Administrator",
            role = UserRole.ADMIN
        )

    fun userBob(): User =
        User(
            id = 1,
            _username = "bob-the-tester",
            joinedAt = Instant.now(),
            fullName = "alice-the-tester",
            role = UserRole.USER
        )

    fun userAlice(): User =
        User(
            id = 2,
            _username = "alice-the-tester",
            joinedAt = Instant.now(),
            fullName = "bob-the-tester",
            role = UserRole.USER
        )
}