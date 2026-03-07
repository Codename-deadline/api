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
    lateinit var permissionService: PermissionService

    @InjectMockKs
    lateinit var rolesService: RolesService

    private val dummyUserBob: User = mockk()
    private val dummyUserScopeBob: UserScope = mockk()

    private val dummyUserAlice: User = mockk()
    private val dummyUserScopeAlice: UserScope = spyk<UserScope>()

    private val scopeId: Long = 0

    @BeforeEach
    fun commonHappyStubs() {
        every { dummyUserBob.username } returns "bob-the-tester"
        every { dummyUserAlice.username } returns "alice-the-tester"

        every { dummyUserScopeAlice.role } returns ScopeRole.ORG_MEMBER

        listOf(
            dummyUserBob to dummyUserScopeBob,
            dummyUserAlice to dummyUserScopeAlice
        ).forEach {
            every {
                permissionLookupService.getHighestRoleUserScope(it.first, scopeId, any())
            } returns Optional.of(it.second)
        }
        every {
            userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase("alice-the-tester", scopeId)
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
                ReflectionTestUtils.invokeMethod(rolesService, "filterRolesByPrefix", "ORG")
            )

        @Test
        fun `filterRolesByPrefix with prefix=THREAD returns Thread roles`() =
            assertEquals(
                threadRoles,
                ReflectionTestUtils.invokeMethod(rolesService, "filterRolesByPrefix", "THR")
            )

        @Test
        fun `filterRolesByPrefix with prefix=DEADLINE returns Deadline roles`() =
            assertEquals(
                deadlineRoles,
                ReflectionTestUtils.invokeMethod(rolesService, "filterRolesByPrefix", "DDL")
            )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ChangeRole {
        private val savedUserScopeSlot: CapturingSlot<UserScope> = slot()
        private val canManageChecks = listOf(
            permissionService::canManageOrganizationMembers,
            permissionService::canManageThreadAssignees,
            permissionService::canManageDeadlineAssignees
        )

        fun scopeRoleScopeTypePairs(): List<Arguments> = listOf(
            Arguments.of(ScopeRole.ORG_OWNER, ScopeType.ORGANIZATION),
            Arguments.of(ScopeRole.THR_ASSIGNEE, ScopeType.THREAD),
            Arguments.of(ScopeRole.DDL_ASSIGNEE, ScopeType.DEADLINE),
        )

        @BeforeEach
        fun commonHappyStubs() {
            every { userScopeRepository.save(capture(savedUserScopeSlot)) } returnsArgument 0

            canManageChecks.forEach { fn ->
                every { fn(any(), any()) } returns true
            }
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
                    dummyUserBob, scopeId, dummyUserBob.username,
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
                    dummyUserBob, scopeId, dummyUserAlice.username,
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
            every { permissionService.canChangeRole(dummyUserBob, ScopeRole.ORG_OWNER, any()) } returns false

            val res = assertThrows<StatusCodeException> {
                rolesService.changeRole(
                    dummyUserBob, scopeId, dummyUserAlice.username,
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
            newRole: ScopeRole, scopeType: ScopeType
        ) {
            canManageChecks.forEach { fn ->
                every { fn(any(), any()) } returns false
            }

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
            every { userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase(any(), any()) } returns Optional.empty()

            val res = assertThrows<StatusCodeException> {
                rolesService.changeRole(
                    dummyUserBob, scopeId, dummyUserAlice.username,
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
                dummyUserBob, scopeId, dummyUserAlice.username,
                ScopeRole.ORG_MEMBER, ScopeType.ORGANIZATION
            )
            assertFalse(savedUserScopeSlot.isCaptured)
        }

        @Test
        fun `happy path commits updated role to db`() {
            rolesService.changeRole(
                dummyUserBob, scopeId, dummyUserAlice.username,
                ScopeRole.ORG_ADMIN, ScopeType.ORGANIZATION
            )
            assertTrue(savedUserScopeSlot.isCaptured)
        }
    }
}