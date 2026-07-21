package xyz.om3lette.deadlines_api.services

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.db.constraintViolation
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import xyz.om3lette.deadlines_api.configs.properties.DeadlinesProperties
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.notifications.DeadlineNotificationPlannerService
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeadlineServiceTest {
    private val maxAssignees = 2L
    private val userScopeRepository: UserScopeRepository = mockk()
    private val threadRepository: ThreadRepository = mockk()
    private val deadlineRepository: DeadlineRepository = mockk()
    private val deadlineNotificationPlannerService: DeadlineNotificationPlannerService = mockk()
    private val permissionService: PermissionService = mockk()
    private val deadlineService = DeadlineService(
        DeadlinesProperties(maxAssignees = maxAssignees),
        userScopeRepository,
        threadRepository,
        deadlineRepository,
        deadlineNotificationPlannerService,
        permissionService
    )

    private lateinit var issuer: User
    private lateinit var assignee: User
    private lateinit var organization: Organization
    private lateinit var thread: Thread
    private lateinit var deadline: Deadline
    private lateinit var organizationMembership: UserScope

    private fun deadlineScope() = DeadlineScope(deadline)

    @BeforeEach
    fun commonHappyStubs() {
        issuer = DomainObjectBuilder.userBob()
        assignee = DomainObjectBuilder.userAlice()
        organization = DomainObjectBuilder.organization()
        thread = DomainObjectBuilder.thread(organization)
        deadline = DomainObjectBuilder.deadline(thread)
        organizationMembership = DomainObjectBuilder.userScope(
            user = assignee,
            scopeType = ScopeType.ORGANIZATION,
            scopeId = organization.id,
            role = ScopeRole.ORG_MEMBER
        )

        every { deadlineRepository.findById(deadline.id) } returns Optional.of(deadline)
    }

    @Nested
    inner class AddAssignee {
        private val savedUserScopeSlot: CapturingSlot<UserScope> = slot()

        @BeforeEach
        fun commonHappyStubs() {
            every {
                userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase(
                    assignee.username,
                    ScopeType.ORGANIZATION,
                    organization.id
                )
            } returns Optional.of(organizationMembership)
            every { permissionService.canAddAssignees(issuer, deadlineScope()) } returns true
            every { userScopeRepository.countDeadlineAssignees(deadline.id) } returns 0
            every { userScopeRepository.saveAndFlush(capture(savedUserScopeSlot)) } returnsArgument 0
        }

        @Test
        fun `invalid deadline role throws 400`() {
            val error = assertThrows<StatusCodeException> {
                deadlineService.addAssignee(issuer, deadline.id, assignee.username, ScopeRole.THR_ASSIGNEE)
            }

            assertAll(
                { assertEquals(400, error.statusCode) },
                { assertEquals(ErrorCode.INVITATION_INVALID_ROLE, error.code) },
                { verify(exactly = 0) { userScopeRepository.saveAndFlush(any()) } }
            )
        }

        @Test
        fun `assigning issuer throws 400`() {
            val error = assertThrows<StatusCodeException> {
                deadlineService.addAssignee(issuer, deadline.id, issuer.username.uppercase(), ScopeRole.DDL_ASSIGNEE)
            }

            assertAll(
                { assertEquals(400, error.statusCode) },
                { assertEquals(ErrorCode.INVITATION_SELF_INVITE, error.code) },
                { verify(exactly = 0) { userScopeRepository.saveAndFlush(any()) } }
            )
        }

        @Test
        fun `missing deadline throws 404`() {
            every { deadlineRepository.findById(deadline.id) } returns Optional.empty()

            val error = assertThrows<StatusCodeException> {
                deadlineService.addAssignee(issuer, deadline.id, assignee.username, ScopeRole.DDL_ASSIGNEE)
            }

            assertAll(
                { assertEquals(404, error.statusCode) },
                { assertEquals(ErrorCode.DDL_NOT_FOUND, error.code) },
                { verify(exactly = 0) { userScopeRepository.saveAndFlush(any()) } }
            )
        }

        @Test
        fun `assignee outside organization throws 400`() {
            every {
                userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase(
                    assignee.username,
                    ScopeType.ORGANIZATION,
                    organization.id
                )
            } returns Optional.empty()

            val error = assertThrows<StatusCodeException> {
                deadlineService.addAssignee(issuer, deadline.id, assignee.username, ScopeRole.DDL_ASSIGNEE)
            }

            assertAll(
                { assertEquals(400, error.statusCode) },
                { assertEquals(ErrorCode.INVITATION_NOT_ORG_MEMBER, error.code) },
                { verify(exactly = 0) { userScopeRepository.saveAndFlush(any()) } }
            )
        }

        @Test
        fun `insufficient permissions throws 403`() {
            every { permissionService.canAddAssignees(issuer, deadlineScope()) } returns false

            val error = assertThrows<StatusCodeException> {
                deadlineService.addAssignee(issuer, deadline.id, assignee.username, ScopeRole.DDL_ASSIGNEE)
            }

            assertAll(
                { assertEquals(403, error.statusCode) },
                { assertEquals(ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS, error.code) },
                { verify(exactly = 0) { userScopeRepository.saveAndFlush(any()) } }
            )
        }

        @Test
        fun `assignee limit reached throws 409`() {
            every { userScopeRepository.countDeadlineAssignees(deadline.id) } returns maxAssignees

            val error = assertThrows<StatusCodeException> {
                deadlineService.addAssignee(issuer, deadline.id, assignee.username, ScopeRole.DDL_ASSIGNEE)
            }

            assertAll(
                { assertEquals(409, error.statusCode) },
                { assertEquals(ErrorCode.DDL_ASSIGNEE_LIMIT_EXCEEDED, error.code) },
                { verify(exactly = 0) { userScopeRepository.saveAndFlush(any()) } }
            )
        }

        @Test
        fun `already assigned member throws 409`() {
            every { userScopeRepository.saveAndFlush(any()) } throws
                constraintViolation(DatabaseConstraint.PK_USER_SCOPES)

            val error = assertThrows<StatusCodeException> {
                deadlineService.addAssignee(issuer, deadline.id, assignee.username, ScopeRole.DDL_ASSIGNEE)
            }

            assertAll(
                { assertEquals(409, error.statusCode) },
                { assertEquals(ErrorCode.MEMBER_ALREADY_ASSIGNED, error.code) }
            )
        }

        @Test
        fun `happy path saves deadline assignee scope`() {
            deadlineService.addAssignee(issuer, deadline.id, assignee.username, ScopeRole.DDL_ASSIGNEE)

            assertAll(
                { assertTrue(savedUserScopeSlot.isCaptured) },
                { assertEquals(assignee, savedUserScopeSlot.captured.user) },
                { assertEquals(ScopeType.DEADLINE, savedUserScopeSlot.captured.scopeType) },
                { assertEquals(deadline.id, savedUserScopeSlot.captured.scopeId) },
                { assertEquals(ScopeRole.DDL_ASSIGNEE, savedUserScopeSlot.captured.role) }
            )
        }
    }
}
