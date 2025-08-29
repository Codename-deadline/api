package xyz.om3lette.deadlines_api.services

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Value
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.roleIsEqualOrHigherThan
import xyz.om3lette.deadlines_api.data.user.enums.UserRole
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.user.isAdminOr
import xyz.om3lette.deadlines_api.util.user.isAdminOrHasRoleAnd
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@ExtendWith(MockKExtension::class)
class PermissionServiceTest {
    @InjectMockKs
    lateinit var permissionService: PermissionService

    private val admin = spyk<User>()
    private val nonAdmin = spyk<User>()
    private val userScope = spyk<UserScope>()

    private val optionalUserScope = Optional.of(userScope)
    private val userScopeLazy = { optionalUserScope }

    private val organization = spyk<Organization>()
    private val organizationLazy = { organization }

    @Value("\${users.max-linked-accounts-per-messenger}")
    private var maxLinkedAccountsPerMessenger: Int = 5

    @BeforeEach
    fun commonHappyStubs() {
        every { admin.role } returns UserRole.ADMIN
        every { nonAdmin.role } returns UserRole.USER

        every { userScope.role } returns ScopeRole.ORG_OWNER

        every { organization.type } returns OrganizationType.PUBLIC
    }

    private fun testForMinAcceptableRole(
        minRole: ScopeRole,
        method: (User, () -> Optional<UserScope>) -> Boolean
    ) {
        assertTrue{
            every { userScope.role } returns minRole
            method(nonAdmin, userScopeLazy)
        }
        if (minRole == ScopeRole.getLowest()) return
        assertFalse{
            every { userScope.role } returns minRole.getNextLowerRoleOrLowest()
            method(nonAdmin, userScopeLazy)
        }
    }

    @Nested
    inner class IsAdminOr {
        @Test
        fun `returns false for non admin`() = assertFalse(nonAdmin.isAdminOr { false })

        @Test
        fun `returns true for admin`() = assertTrue(admin.isAdminOr { false })
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RoleComparisonTests {

        fun higherLowerPairs(): List<Arguments> = listOf(
            Arguments.of(ScopeRole.ORG_OWNER, ScopeRole.ORG_OWNER, true),
            Arguments.of(ScopeRole.ORG_ADMIN, ScopeRole.ORG_OWNER, false),
            Arguments.of(ScopeRole.ORG_ADMIN, ScopeRole.DDL_ASSIGNEE, true),
            Arguments.of(ScopeRole.THR_ASSIGNEE, ScopeRole.DDL_ASSIGNEE, true),
            Arguments.of(ScopeRole.ORG_MEMBER, ScopeRole.DDL_ASSIGNEE, false)
        )

        @ParameterizedTest
        @MethodSource("higherLowerPairs")
        fun `roleIsEqualOrHigherThan behaves as expected`(currentRole: ScopeRole, requiredRole: ScopeRole, expected: Boolean) {
            every { userScope.role } returns currentRole
            assertEquals(expected, userScope.roleIsEqualOrHigherThan(requiredRole))
        }
    }


    @Nested
    inner class IsAdminOrHasRoleAnd {

        @Test
        fun `returns true for admin regardless of scope`() =
            assertTrue(admin.isAdminOrHasRoleAnd({ Optional.empty() }) { false })

        @Test
        fun `returns false when no scope present and not admin`() =
            assertFalse(nonAdmin.isAdminOrHasRoleAnd({ Optional.empty() }) { true })

        @Test
        fun `returns predicate result when scope present and not admin`() {
            every { userScope.role } returns ScopeRole.ORG_OWNER

            val resultTrue = nonAdmin.isAdminOrHasRoleAnd({ optionalUserScope }) { true }
            val resultFalse = nonAdmin.isAdminOrHasRoleAnd({ optionalUserScope }) { false }

            assertTrue(resultTrue)
            assertFalse(resultFalse)
        }
    }

    @Nested
    inner class OrganizationPermissions {

        @Test
        fun hasOrganizationAccess() {
            assertTrue(
                permissionService.hasOrganizationAccess(nonAdmin, organization) { Optional.empty() },
                "Public organization should be available to everyone"
            )

            every { organization.type } returns OrganizationType.PRIVATE
            assertFalse (
                permissionService.hasOrganizationAccess(nonAdmin, organization) { Optional.empty() },
                "Private organization should not be accessible by non members"
            )

            every { userScope.role } returns ScopeRole.ORG_MEMBER
            assertTrue(
                permissionService.hasOrganizationAccess(nonAdmin, organization) { optionalUserScope },
                "Private organization should be available to ORG_MEMBER or higher"
            )
        }

        @Test
        fun canDeleteOrganization() = testForMinAcceptableRole(ScopeRole.ORG_OWNER, permissionService::canDeleteOrganization)

        @Test
        fun canUpdateOrganization() = testForMinAcceptableRole(ScopeRole.ORG_OWNER, permissionService::canUpdateOrganization)

        @Test
        fun canManageOrganizationMembers() = testForMinAcceptableRole(ScopeRole.ORG_ADMIN, permissionService::canManageOrganizationMembers)
    }

    @Nested
    inner class ThreadPermissions {

        @Test
        fun hasThreadAccess() {
            assertTrue(
                permissionService.hasThreadAccess(nonAdmin, { Optional.empty() }, organizationLazy),
                "Thread inside of public a organization should be available to everyone"
            )

            every { organization.type } returns OrganizationType.PRIVATE
            assertFalse (
                permissionService.hasThreadAccess(nonAdmin, { Optional.empty() }, organizationLazy),
                "Thread inside of a private organization should not be accessible by non members"
            )

            every { userScope.role } returns ScopeRole.THR_ASSIGNEE
            assertTrue(
                permissionService.hasThreadAccess(nonAdmin, userScopeLazy, organizationLazy),
                "Threads inside of private organization should be available to THR_ASSIGNEE or higher"
            )
        }

        @Test
        fun canCreateOrDeleteThread() = testForMinAcceptableRole(ScopeRole.ORG_ADMIN, permissionService::canCreateOrDeleteThread)

        @Test
        fun canUpdateThread() = testForMinAcceptableRole(ScopeRole.ORG_ADMIN, permissionService::canUpdateThread)

        @Test
        fun canManageThreadAssignees() = testForMinAcceptableRole(ScopeRole.ORG_ADMIN, permissionService::canManageThreadAssignees)
    }

    @Nested
    inner class DeadlinePermissions {

        @Test
        fun hasDeadlineAccess() {
            assertTrue(
                permissionService.hasDeadlineAccess(nonAdmin, { Optional.empty() }, organization),
                "Deadline inside of public a organization should be available to everyone"
            )

            every { organization.type } returns OrganizationType.PRIVATE
            assertFalse (
                permissionService.hasDeadlineAccess(nonAdmin, { Optional.empty() }, organization),
                "Deadline inside of a private organization should not be accessible by non members"
            )

            every { userScope.role } returns ScopeRole.DDL_ASSIGNEE
            assertTrue(
                permissionService.hasDeadlineAccess(nonAdmin, userScopeLazy, organization),
                "Deadline inside of private organization should be available to THR_ASSIGNEE or higher"
            )
        }

        @Test
        fun canCreateOrDeleteDeadline() = testForMinAcceptableRole(ScopeRole.THR_ASSIGNEE, permissionService::canCreateOrDeleteDeadline)

        @Test
        fun canUpdateDeadline() = testForMinAcceptableRole(ScopeRole.THR_ASSIGNEE, permissionService::canUpdateDeadline)

        @Test
        fun canManageDeadlineAssignees() = testForMinAcceptableRole(ScopeRole.THR_ASSIGNEE, permissionService::canManageDeadlineAssignees)

        @Test
        fun canManageDeadlineAttachments() = testForMinAcceptableRole(ScopeRole.DDL_ASSIGNEE, permissionService::canManageDeadlineAttachments)
    }

    @Nested
    inner class OrganizationInvitation {
        @Test
        fun canSendOrganizationInvitation() = testForMinAcceptableRole(ScopeRole.ORG_ADMIN, permissionService::canSendOrganizationInvitation)
    }

    @Nested
    inner class Integration {
        @Test
        fun canLinkAccount() {
            assertTrue(
                permissionService.canLinkAccount(
                    nonAdmin,
                    maxLinkedAccountsPerMessenger - 1
                )
            )
            assertFalse(
                permissionService.canLinkAccount(
                    nonAdmin,
                    maxLinkedAccountsPerMessenger
                )
            )
        }
    }

    @Nested
    inner class Roles {
        @Test
        fun canChangeRole() {
            assertFalse(
                permissionService.canChangeRole(
                    nonAdmin, ScopeRole.ORG_MEMBER
                ) { Optional.empty() }
            )
            assertTrue(
                permissionService.canChangeRole(
                    nonAdmin, ScopeRole.ORG_ADMIN, userScopeLazy
                )
            )

            every { userScope.role } returns ScopeRole.ORG_MEMBER
            assertFalse(
                permissionService.canChangeRole(
                    nonAdmin, ScopeRole.THR_ASSIGNEE, userScopeLazy
                )
            )
        }
    }
}