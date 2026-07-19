package xyz.om3lette.deadlines_api.services

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
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
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.data.permissions.dto.PermissionRoleDTO
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
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
    lateinit var organizationRepository: OrganizationRepository

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

    private lateinit var dummyUserAlice: User
    private lateinit var dummyUserScopeAlice: UserScope

    @BeforeEach
    fun commonHappyStubs() {
        organization = DomainObjectBuilder.organization()
        thread = DomainObjectBuilder.thread(organization)
        deadline = DomainObjectBuilder.deadline(thread)
        every { threadsRepository.findById(thread.id) } returns Optional.of(thread)
        every { deadlineRepository.findById(deadline.id) } returns Optional.of(deadline)
        every { organizationRepository.findByIdForUpdate(organization.id) } returns organization

        dummyUserBob = DomainObjectBuilder.userBob()
        dummyUserAlice = DomainObjectBuilder.userAlice()
        dummyUserScopeAlice = DomainObjectBuilder.userScope(
            user = dummyUserAlice,
            scopeType = ScopeType.ORGANIZATION,
            scopeId = organization.id,
            role = ScopeRole.ORG_MEMBER
        )

        every {
            userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase(
                dummyUserAlice.username, any(), organization.id
            )
        } returns Optional.of(dummyUserScopeAlice)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ChangeRole {
        private val savedUserScopeSlot: CapturingSlot<UserScope> = slot()

        fun scopeRoleScopeTypePairs(): List<Arguments> = listOf(
            Arguments.of(ScopeRole.ORG_ADMIN, organization.id, ScopeType.ORGANIZATION),
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
        fun `assigning organization owner through generic role change throws StatusCodeException 400`() {
            val res = assertThrows<StatusCodeException> {
                rolesService.changeRole(
                    dummyUserBob, organization.id, dummyUserAlice.username,
                    ScopeRole.ORG_OWNER, ScopeType.ORGANIZATION
                )
            }
            assertAll(
                { assertEquals(400, res.statusCode) },
                { assertEquals(ErrorCode.ROLE_IMPLICIT_OWNERSHIP_CHANGE, res.code) },
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
                    ScopeRole.ORG_ADMIN, ScopeType.ORGANIZATION
                )
            }
            assertAll(
                { assertEquals(400, res.statusCode) },
                { assertFalse(savedUserScopeSlot.isCaptured) }
            )
        }

        @Test
        fun `demoting organization owner through generic role change throws StatusCodeException 400`() {
            dummyUserScopeAlice.role = ScopeRole.ORG_OWNER

            val error = assertThrows<StatusCodeException> {
                rolesService.changeRole(
                    dummyUserBob,
                    organization.id,
                    dummyUserAlice.username,
                    ScopeRole.ORG_ADMIN,
                    ScopeType.ORGANIZATION
                )
            }

            assertAll(
                { assertEquals(400, error.statusCode) },
                { assertEquals(ErrorCode.ROLE_IMPLICIT_OWNERSHIP_CHANGE, error.code) },
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
            assertEquals(ScopeRole.ORG_ADMIN, savedUserScopeSlot.captured.role)
        }
    }

    @Nested
    inner class ChangeOrganizationOwner {
        private lateinit var issuerRole: PermissionRoleDTO
        private lateinit var newOwnerRole: PermissionRoleDTO

        @BeforeEach
        fun commonHappyStubs() {
            issuerRole = PermissionRoleDTO(dummyUserBob.id, ScopeRole.ORG_OWNER)
            newOwnerRole = PermissionRoleDTO(dummyUserAlice.id, ScopeRole.ORG_ADMIN)
            every {
                userScopeRepository.findOrganizationRolesForOwnerTransfer(
                    dummyUserBob.id,
                    dummyUserAlice.username.lowercase(),
                    organization.id
                )
            } returns listOf(issuerRole, newOwnerRole)
            every {
                userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
                    any(), any(), any(), any()
                )
            } returns 1
        }

        @Test
        fun `happy path demotes issuer and promotes selected member`() {
            rolesService.changeOrganizationOwner(
                issuer = dummyUserBob,
                organizationId = organization.id,
                newOwnerUsername = dummyUserAlice.username.uppercase()
            )

            verifyOrder {
                organizationRepository.findByIdForUpdate(organization.id)
                userScopeRepository.findOrganizationRolesForOwnerTransfer(
                    dummyUserBob.id,
                    dummyUserAlice.username.lowercase(),
                    organization.id
                )
                userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
                    dummyUserBob.id,
                    ScopeRole.ORG_ADMIN,
                    organization.id,
                    ScopeType.ORGANIZATION
                )
                userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
                    dummyUserAlice.id,
                    ScopeRole.ORG_OWNER,
                    organization.id,
                    ScopeType.ORGANIZATION
                )
            }
        }

        @Test
        fun `transferring ownership to issuer throws 400`() {
            val error = assertThrows<StatusCodeException> {
                rolesService.changeOrganizationOwner(
                    issuer = dummyUserBob,
                    organizationId = organization.id,
                    newOwnerUsername = dummyUserBob.username.uppercase()
                )
            }

            assertAll(
                { assertEquals(400, error.statusCode) },
                { assertEquals(ErrorCode.ROLE_CHANGE_SELF, error.code) },
                { verify(exactly = 0) { organizationRepository.findByIdForUpdate(any()) } },
                {
                    verify(exactly = 0) {
                        userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
                            any(), any(), any(), any()
                        )
                    }
                }
            )
        }

        @Test
        fun `missing organization throws 404`() {
            every { organizationRepository.findByIdForUpdate(organization.id) } returns null

            val error = assertThrows<StatusCodeException> {
                rolesService.changeOrganizationOwner(
                    issuer = dummyUserBob,
                    organizationId = organization.id,
                    newOwnerUsername = dummyUserAlice.username
                )
            }

            assertAll(
                { assertEquals(404, error.statusCode) },
                { assertEquals(ErrorCode.ORG_NOT_FOUND, error.code) },
                {
                    verify(exactly = 0) {
                        userScopeRepository.findOrganizationRolesForOwnerTransfer(
                            any(), any(), any()
                        )
                    }
                }
            )
        }

        @Test
        fun `issuer without owner role throws 403`() {
            every {
                userScopeRepository.findOrganizationRolesForOwnerTransfer(
                    any(), any(), any()
                )
            } returns listOf(
                PermissionRoleDTO(dummyUserBob.id, ScopeRole.ORG_ADMIN),
                newOwnerRole
            )

            val error = assertThrows<StatusCodeException> {
                rolesService.changeOrganizationOwner(
                    issuer = dummyUserBob,
                    organizationId = organization.id,
                    newOwnerUsername = dummyUserAlice.username
                )
            }

            assertAll(
                { assertEquals(403, error.statusCode) },
                { assertEquals(ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS, error.code) },
                {
                    verify(exactly = 0) {
                        userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
                            any(), any(), any(), any()
                        )
                    }
                }
            )
        }

        @Test
        fun `target without organization membership throws 404`() {
            every {
                userScopeRepository.findOrganizationRolesForOwnerTransfer(
                    any(), any(), any()
                )
            } returns listOf(issuerRole)

            val error = assertThrows<StatusCodeException> {
                rolesService.changeOrganizationOwner(
                    dummyUserBob, organization.id, dummyUserAlice.username
                )
            }

            assertAll(
                { assertEquals(404, error.statusCode) },
                { assertEquals(ErrorCode.MEMBER_NOT_FOUND, error.code) },
                {
                    verify(exactly = 0) {
                        userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
                            any(), any(), any(), any()
                        )
                    }
                }
            )
        }

        @Test
        fun `missing issuer role during update throws 500 and skips promotion`() {
            every {
                userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
                    dummyUserBob.id,
                    ScopeRole.ORG_ADMIN,
                    organization.id,
                    ScopeType.ORGANIZATION
                )
            } returns 0

            val error = assertThrows<StatusCodeException> {
                rolesService.changeOrganizationOwner(
                    issuer = dummyUserBob,
                    organizationId = organization.id,
                    newOwnerUsername = dummyUserAlice.username
                )
            }

            assertAll(
                { assertEquals(500, error.statusCode) },
                { assertEquals(ErrorCode.UNKNOWN_ERROR, error.code) },
                {
                    verify(exactly = 0) {
                        userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
                            dummyUserAlice.id,
                            ScopeRole.ORG_OWNER,
                            organization.id,
                            ScopeType.ORGANIZATION
                        )
                    }
                }
            )
        }

        @Test
        fun `missing target role during update throws 500`() {
            every {
                userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
                    dummyUserAlice.id,
                    ScopeRole.ORG_OWNER,
                    organization.id,
                    ScopeType.ORGANIZATION
                )
            } returns 0

            val error = assertThrows<StatusCodeException> {
                rolesService.changeOrganizationOwner(
                    issuer = dummyUserBob,
                    organizationId = organization.id,
                    newOwnerUsername = dummyUserAlice.username
                )
            }

            assertAll(
                { assertEquals(500, error.statusCode) },
                { assertEquals(ErrorCode.UNKNOWN_ERROR, error.code) }
            )
        }
    }
}
