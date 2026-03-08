package xyz.om3lette.deadlines_api.services

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.util.ReflectionTestUtils
import xyz.om3lette.deadlines_api.DomainObjectBuilder
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
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class RolesServiceTest {
    @MockK
    lateinit var userScopeRepository: UserScopeRepository

    @MockK
    lateinit var deadlineRepository: DeadlineRepository

    @MockK
    lateinit var threadsRepository: ThreadRepository

    @MockK
    lateinit var permissionService: PermissionService

    @InjectMockKs
    lateinit var rolesService: RolesService

    private lateinit var organization: Organization
    private lateinit var thread: Thread
    private lateinit var deadline: Deadline

    private lateinit var dummyUserBob: User
    private val dummyUserScopeBob: UserScope = mockk()

    private lateinit var dummyUserAlice: User
    private val dummyUserScopeAlice: UserScope = spyk<UserScope>()

    @BeforeEach
    fun commonHappyStubs() {
        organization = DomainObjectBuilder.organization()
        thread = DomainObjectBuilder.thread(organization)
        deadline = DomainObjectBuilder.deadline(organization, thread)
        every { threadsRepository.findById(thread.id) } returns Optional.of(thread)
        every { deadlineRepository.findById(deadline.id) } returns Optional.of(deadline)

        dummyUserBob = DomainObjectBuilder.userBob()
        dummyUserAlice = DomainObjectBuilder.userAlice()

        every { dummyUserScopeAlice.role } returns ScopeRole.ORG_MEMBER
        every {
            userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase(
                dummyUserAlice.username, any(), organization.id
            )
        } returns Optional.of(dummyUserScopeAlice)
    }

    @Nested
    inner class UtilMethods {
        private val orgRoles = listOf(ScopeRole.ORG_MEMBER, ScopeRole.ORG_ADMIN, ScopeRole.ORG_OWNER)

        private val threadRoles = listOf(ScopeRole.THR_ASSIGNEE)

        private val deadlineRoles = listOf(ScopeRole.DDL_ASSIGNEE)

        @Test
        fun `filterRolesByPrefix with prefix=ORG returns Organization roles`() =
            assertEquals(
                orgRoles,
                ReflectionTestUtils.invokeMethod(
                    rolesService, "filterRolesByPrefix", ScopeType.ORGANIZATION.code
                )
            )

        @Test
        fun `filterRolesByPrefix with prefix=THREAD returns Thread roles`() =
            assertEquals(
                threadRoles,
                ReflectionTestUtils.invokeMethod(
                    rolesService, "filterRolesByPrefix", ScopeType.THREAD.code
                )
            )

        @Test
        fun `filterRolesByPrefix with prefix=DEADLINE returns Deadline roles`() =
            assertEquals(
                deadlineRoles,
                ReflectionTestUtils.invokeMethod(
                    rolesService, "filterRolesByPrefix", ScopeType.DEADLINE.code
                )
            )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ChangeRole {
        private val savedUserScopeSlot: CapturingSlot<UserScope> = slot()

        fun scopeRoleScopeTypePairs(): List<Arguments> = listOf(
            Arguments.of(ScopeRole.ORG_OWNER, organization.id, ScopeType.ORGANIZATION),
            Arguments.of(ScopeRole.THR_ASSIGNEE, thread.id, ScopeType.THREAD),
            Arguments.of(ScopeRole.DDL_ASSIGNEE, deadline.id, ScopeType.DEADLINE),
        )

        @BeforeEach
        fun commonHappyStubs() {
            every { userScopeRepository.save(capture(savedUserScopeSlot)) } returnsArgument 0

            every { permissionService.canChangeRole(any(), any(), any()) } returns true
            every {
                permissionService.canChangeRole(
                    dummyUserBob,
                    any(),
                    any()
                )
            } returns true
        }

        @Test
        fun `changing issuer's role throws StatusCodeException 400`() {
            val res = assertThrows<StatusCodeException> {
                rolesService.changeRole(
                    dummyUserBob, organization.id, dummyUserBob.username,
                    ScopeRole.ORG_ADMIN, ScopeType.ORGANIZATION
                )
            }
            assertAll(
                { assertEquals(400, res.statusCode) },
                { assertFalse(savedUserScopeSlot.isCaptured) }
            )
        }

        @Test
        fun `role not in returned by filterRolesByPrefix throws StatusCodeException 400`() {
            val res = assertThrows<StatusCodeException> {
                rolesService.changeRole(
                    dummyUserBob, organization.id, dummyUserAlice.username,
                    ScopeRole.DDL_ASSIGNEE, ScopeType.ORGANIZATION
                )
            }
            assertAll(
                { assertEquals(400, res.statusCode) },
                { assertFalse(savedUserScopeSlot.isCaptured) }
            )
        }

        @Test
        fun `attempting to promote to a higher role than issuer's throws StatusCodeException 403`() {
            every {
                permissionService.canChangeRole(
                    dummyUserBob, any(), ScopeRole.ORG_OWNER
                )
            } returns false

            val res = assertThrows<StatusCodeException> {
                rolesService.changeRole(
                    dummyUserBob, organization.id, dummyUserAlice.username,
                    ScopeRole.ORG_OWNER, ScopeType.ORGANIZATION
                )
            }
            assertAll(
                { assertEquals(403, res.statusCode) },
                { assertFalse(savedUserScopeSlot.isCaptured) }
            )
        }

        @ParameterizedTest
        @MethodSource("scopeRoleScopeTypePairs")
        fun `not enough permissions to manage roles throws StatusCodeException 403`(
            newRole: ScopeRole, scopeId: Long, scopeType: ScopeType
        ) {
            every { permissionService.canChangeRole(any(), any(), newRole) } returns false

            val res = assertThrows<StatusCodeException> {
                rolesService.changeRole(
                    dummyUserAlice, scopeId, dummyUserBob.username,
                    newRole, scopeType
                )
            }
            assertAll(
                { assertEquals(403, res.statusCode) },
                { assertFalse(savedUserScopeSlot.isCaptured) }
            )
        }

        @Test
        fun `subject UserScope not found throws StatusCodeException 400`() {
            every {
                userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase(
                    any(), any(), any()
                )
            } returns Optional.empty()

            val res = assertThrows<StatusCodeException> {
                rolesService.changeRole(
                    dummyUserBob, organization.id, dummyUserAlice.username,
                    ScopeRole.ORG_OWNER, ScopeType.ORGANIZATION
                )
            }
            assertAll(
                { assertEquals(400, res.statusCode) },
                { assertFalse(savedUserScopeSlot.isCaptured) }
            )
        }

        @Test
        fun `happy path if new role equals to the old one no db request happens`() {
            rolesService.changeRole(
                dummyUserBob, organization.id, dummyUserAlice.username,
                ScopeRole.ORG_MEMBER, ScopeType.ORGANIZATION
            )
            assertFalse(savedUserScopeSlot.isCaptured)
        }

        @Test
        fun `happy path commits updated role to db`() {
            rolesService.changeRole(
                dummyUserBob, organization.id, dummyUserAlice.username,
                ScopeRole.ORG_ADMIN, ScopeType.ORGANIZATION
            )
            assertTrue(savedUserScopeSlot.isCaptured)
        }
    }
}