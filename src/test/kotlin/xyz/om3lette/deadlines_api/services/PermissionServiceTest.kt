package xyz.om3lette.deadlines_api.services

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Value
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.PermissionScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.PermissionsRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.roleIsEqualOrHigherThan
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.permission.PermissionContext
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.user.isAdminOr
import xyz.om3lette.deadlines_api.util.user.isAdminOrHasRoleAnd
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@ExtendWith(MockKExtension::class)
class PermissionServiceTest {
    @MockK
    lateinit var permissionsRepository: PermissionsRepository

    @MockK
    lateinit var permissionContext: PermissionContext

    @InjectMockKs
    lateinit var permissionService: PermissionService

    private val admin = DomainObjectBuilder.admin()
    private val nonAdmin = DomainObjectBuilder.user()
    private val userScope = spyk<UserScope>()

    private lateinit var organization: Organization
    private lateinit var thread: Thread
    private lateinit var deadline: Deadline

    fun orgScope() = OrganizationScope(organization.id, organization)
    fun thrScope() = ThreadScope(thread)
    fun ddlScope() = DeadlineScope(deadline)

    @Value("\${users.max-linked-accounts-per-messenger}")
    private var maxLinkedAccountsPerMessenger: Int = 5

    @BeforeEach
    fun commonHappyStubs() {
        organization = DomainObjectBuilder.organization()
        thread = DomainObjectBuilder.thread(organization)
        deadline = DomainObjectBuilder.deadline(organization, thread)

        // Cache passthrough
        every {
            permissionContext.getOrLoad(any(), any())
        } answers {
            secondArg<() -> ScopeRole?>()()
        }
    }

    fun withRoleScope(user: User, permissionScope: PermissionScope, role: ScopeRole) {
        when (permissionScope) {
            is OrganizationScope -> withRole(user, orgId = permissionScope.orgId, role = role)
            is ThreadScope -> withRole(user, thread = permissionScope.thread, role = role)
            is DeadlineScope -> withRole(user, deadline = permissionScope.deadline, role = role)
        }
    }

    fun withRole(
        user: User,
        orgId: Long? = null,
        thread: Thread? = null,
        deadline: Deadline? = null,
        role: ScopeRole?,
        useAny: Boolean = false,
    ) {
        assertTrue(
            listOf(orgId, thread, deadline).count { it != null } <= 1,
            "Only one of ORG, THR, DDL or none can be supplied"
        )

        var curOrgId: Long? = orgId
        var thrId: Long? = null
        var ddlId: Long? = null

        if (thread != null) {
            curOrgId = thread.organization.id
            thrId = thread.id
        } else if (deadline != null) {
            curOrgId = deadline.organization.id
            thrId = deadline.thread.id
            ddlId = deadline.id
        }
        every {
            permissionsRepository.findHighestRoleByUser(
                user.id,
                if (useAny && curOrgId == null) any() else curOrgId,
                if (useAny && thrId == null) any() else thrId,
                if (useAny && ddlId == null) any() else ddlId
            )
        } returns role
    }

    private fun testForMinAcceptableRole(
        minRole: ScopeRole,
        permissionScope: PermissionScope,
        method: (User, PermissionScope) -> Boolean
    ) {
        withRoleScope(nonAdmin, permissionScope, minRole)
        assertTrue{ method(nonAdmin, permissionScope) }
        if (minRole == ScopeRole.getLowest()) return

        withRoleScope(nonAdmin, permissionScope, minRole.getNextLowerRoleOrLowest())
        assertFalse{ method(nonAdmin, permissionScope) }
    }

    /**
     * Tests minimal acceptable role for function which do not accept `PermissionScope`.
     *
     * This means that the function is entity specific and cannot be generalized to use a `PermissionScope`.
     */
    private fun <T> testForMinAcceptableRoleRaw(
        minRole: ScopeRole,
        target: T,
        method: (User, T) -> Boolean
    ) {
        withRole(nonAdmin, role = minRole, useAny = true)
        assertTrue { method(nonAdmin, target) }

        if (minRole == ScopeRole.getLowest()) return

        withRole(nonAdmin, role = minRole.getNextLowerRoleOrLowest(), useAny = true)
        assertFalse { method(nonAdmin, target) }
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
            assertEquals(expected, currentRole >= requiredRole)
            assertEquals(expected, userScope.roleIsEqualOrHigherThan(requiredRole))
        }
    }


    @Nested
    inner class IsAdminOrHasRoleAnd {

        @Test
        fun `returns true for admin regardless of scope`() =
            assertTrue(admin.isAdminOrHasRoleAnd({ null }) { false })

        @Test
        fun `returns false when no scope present and not admin`() =
            assertFalse(nonAdmin.isAdminOrHasRoleAnd({ null }) { true })

        @Test
        fun `returns predicate result when scope present and not admin`() {
            val resultTrue = nonAdmin.isAdminOrHasRoleAnd({ ScopeRole.ORG_OWNER }) { true }
            val resultFalse = nonAdmin.isAdminOrHasRoleAnd({ ScopeRole.ORG_OWNER }) { false }

            assertTrue(resultTrue)
            assertFalse(resultFalse)
        }
    }

    @Nested
    inner class OrganizationPermissions {
        @Nested
        inner class HasOrganizationAccess {

            @Test
            fun `public organization is available regardless of membership`() {
                withRole(nonAdmin, orgId = organization.id, role = null)
                assertTrue(permissionService.hasAccess(nonAdmin, orgScope()))
            }

            @Test
            fun `private organization are not available if user is not a member`() {
                organization.type = OrganizationType.PRIVATE
                withRole(nonAdmin, orgId = organization.id, role = null)
                assertFalse (
                    permissionService.hasAccess(nonAdmin, orgScope()),
                    "Private organization should not be accessible by non members"
                )

                withRole(nonAdmin, orgId = organization.id, role = ScopeRole.ORG_MEMBER)
                assertTrue(
                    permissionService.hasAccess(nonAdmin, orgScope()),
                    "Private organization should be available to ORG_MEMBER or higher"
                )
            }
        }

        @Test
        fun canDeleteOrganization() = testForMinAcceptableRole(
            ScopeRole.ORG_OWNER, orgScope(), permissionService::canDelete
        )

        @Test
        fun canUpdateOrganization() = testForMinAcceptableRole(
            ScopeRole.ORG_OWNER, orgScope(), permissionService::canUpdate
        )

        @Test
        fun canManageOrganizationMembers() = testForMinAcceptableRole(
            ScopeRole.ORG_ADMIN, orgScope(), permissionService::canManageAssignees
        )
    }

    @Nested
    inner class ThreadPermissions {
        @Nested
        inner class HasThreadAccess {

            @Test
            fun `threads in public organization are accessible by everyone`() {
                withRole(nonAdmin, thread = thread, role = null)
                assertTrue(
                    permissionService.hasAccess(nonAdmin, thrScope()),
                    "Thread inside of public a organization should be available to everyone"
                )
            }

            @Test
            fun `threads in private organizations are accessible only by org members`() {
                organization.type = OrganizationType.PRIVATE
                withRole(nonAdmin, thread = thread, role = null)

                assertFalse (
                    permissionService.hasAccess(nonAdmin, thrScope()),
                    "Thread inside of a private organization should not be accessible by non members"
                )

                withRole(nonAdmin, thread = thread, role = ScopeRole.THR_ASSIGNEE)
                assertTrue(
                    permissionService.hasAccess(nonAdmin, thrScope()),
                    "Threads inside of private organization should be available to THR_ASSIGNEE or higher"
                )
            }
        }

        @Test
        fun canCreateOrDeleteThread() =
            testForMinAcceptableRole(ScopeRole.ORG_ADMIN, thrScope(), permissionService::canDelete)

        @Test
        fun canUpdateThread() =
            testForMinAcceptableRole(
                ScopeRole.ORG_ADMIN, thrScope(),permissionService::canUpdate
            )

        @Test
        fun canManageThreadAssignees() = testForMinAcceptableRole(
            ScopeRole.ORG_ADMIN, thrScope(),permissionService::canManageAssignees
        )
    }

    @Nested
    inner class DeadlinePermissions {
        @Nested
        inner class HasDeadlineAccess {
            @Test
            fun `deadlines in public org are accessible by everyone`() {
                withRole(nonAdmin, deadline = deadline, role = null)
                assertTrue(permissionService.hasAccess(nonAdmin, ddlScope()))
            }

            @Test
            fun `deadlines in private organizations are only accessible by deadline assignees or higher`() {
                organization.type = OrganizationType.PRIVATE
                withRole(nonAdmin, deadline = deadline, role = null)

                assertFalse (permissionService.hasAccess(nonAdmin, ddlScope()))

                withRole(nonAdmin, deadline = deadline, role = ScopeRole.DDL_ASSIGNEE)
                assertTrue(permissionService.hasAccess(nonAdmin, ddlScope()))
            }
        }

        @Test
        fun canCreateOrDeleteDeadline() = testForMinAcceptableRoleRaw(
            ScopeRole.THR_ASSIGNEE, thread, permissionService::canCreateDeadline
        )

        @Test
        fun canUpdateDeadline() = testForMinAcceptableRole(
            ScopeRole.THR_ASSIGNEE, ddlScope(), permissionService::canUpdate
        )

        @Test
        fun canManageDeadlineAssignees() = testForMinAcceptableRole(
            ScopeRole.THR_ASSIGNEE, ddlScope(), permissionService::canManageAssignees
        )

        @Test
        fun canManageDeadlineAttachments() = testForMinAcceptableRoleRaw(
            ScopeRole.DDL_ASSIGNEE, deadline, permissionService::canManageDeadlineAttachments
        )
    }

    @Nested
    inner class OrganizationInvitation {
        @Test
        fun canSendOrganizationInvitation() = testForMinAcceptableRoleRaw(
            ScopeRole.ORG_ADMIN, organization.id, permissionService::canSendOrganizationInvitation
        )
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
            every {
                permissionsRepository.findHighestRoleByUser(
                    nonAdmin.id, any(), any(), any()
                )
            } returns ScopeRole.ORG_OWNER
            assertTrue(
                permissionService.canChangeRole(
                    nonAdmin, orgScope(), ScopeRole.ORG_ADMIN
                )
            )

            every {
                permissionsRepository.findHighestRoleByUser(
                    nonAdmin.id, any(), any(), any()
                )
            } returns ScopeRole.ORG_MEMBER
            assertFalse(
                permissionService.canChangeRole(
                    nonAdmin, orgScope(), ScopeRole.THR_ASSIGNEE
                )
            )
        }
    }
}