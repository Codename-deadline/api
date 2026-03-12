package xyz.om3lette.deadlines_api.services

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationInvitation
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationInvitationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.roleIsEqualOrHigherThan
import xyz.om3lette.deadlines_api.data.user.enums.UserRole
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import java.time.Instant
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class OrganizationInvitationServiceTest {
    private val userRepository: UserRepository = mockk()
    private val userScopeRepository: UserScopeRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()
    private val organizationInvitationRepository: OrganizationInvitationRepository = mockk()
    private val permissionService: PermissionService = mockk()
    private val organizationInvitationService: OrganizationInvitationService = OrganizationInvitationService(
        userRepository,
        userScopeRepository,
        organizationRepository,
        organizationInvitationRepository,
        permissionService
    )

    private val dummyUserBob: User = mockk()
    private val dummyUserScopeBob: UserScope = mockk()

    private val dummyUserAlice: User = mockk()

    private val dummyOrganization: Organization = spyk(Organization(
        42,
        "org",
        null,
        OrganizationType.PUBLIC,
        Instant.now().minusSeconds(120),
        members = mutableListOf(dummyUserScopeBob)
    ))
    private lateinit var dummyInvitation: OrganizationInvitation

    @BeforeEach
    fun commonMock() {
        dummyInvitation = spyk(OrganizationInvitation(
            0,
            dummyUserBob,
            dummyUserAlice,
            dummyOrganization,
            InvitationStatus.PENDING,
            ScopeRole.ORG_ADMIN,
            Instant.now().minusSeconds(60)
        ))

        every { dummyUserBob.username } returns "bob-the-tester"
        every { dummyUserBob.id } returns 0

        every { dummyUserScopeBob.user } returns dummyUserBob
        every { dummyUserScopeBob.role } returns ScopeRole.ORG_OWNER

        every { dummyUserAlice.username } returns "alice-the-tester"
        every { dummyUserAlice.id } returns 1
        every { dummyUserScopeBob.role } returns ScopeRole.ORG_OWNER

        every { organizationInvitationRepository.findById(0) } returns Optional.of(dummyInvitation)
        every { userScopeRepository.findByUserAndScopeIdAndScopeType(
            dummyUserBob, dummyOrganization.id, ScopeType.ORGANIZATION)
        } returns Optional.of(dummyUserScopeBob)
        every { userScopeRepository.findByUserAndScopeIdAndScopeType(
            dummyUserAlice, dummyOrganization.id, ScopeType.ORGANIZATION)
        } returns Optional.empty()


        every { permissionService.canSendOrganizationInvitation(any(), any()) } returns true

        every { organizationRepository.findById(0) } returns Optional.of(dummyOrganization)

        val bobUsername = dummyUserBob.username
        val aliceUsername = dummyUserAlice.username
        every { userRepository.findByUsernameIgnoreCase(bobUsername) } returns Optional.of(dummyUserBob)
        every { userRepository.findByUsernameIgnoreCase(aliceUsername) } returns Optional.of(dummyUserAlice)
    }

    @Nested
    inner class InviteUser {
        private val savedInvitationSlot: CapturingSlot<OrganizationInvitation> = slot()

        @BeforeEach
        fun commonHappyStubs() {
            savedInvitationSlot.clear()

            every { organizationInvitationRepository.save(capture(savedInvitationSlot)) } returnsArgument 0
        }

        @Test
        fun `not enough permissions throws StatusCodeException 403`() {
            every { permissionService.canSendOrganizationInvitation(any(), any()) } returns false

            val res = assertThrows<StatusCodeException> {
                organizationInvitationService.createInvitation(
                    dummyUserBob, 0, dummyUserAlice.username, ScopeRole.ORG_MEMBER
                )
            }

            assertAll(
                { assertFalse { savedInvitationSlot.isCaptured } },
                { assertEquals(403, res.statusCode) }
            )
        }

        @Test
        fun `inviting to a personal organization throws StatusCodeException 400`() {
            every { dummyOrganization.type } returns OrganizationType.PERSONAL

            val res = assertThrows<StatusCodeException> {
                organizationInvitationService.createInvitation(
                    dummyUserBob, 0, dummyUserAlice.username, ScopeRole.ORG_MEMBER
                )
            }

            assertAll(
                { assertFalse { savedInvitationSlot.isCaptured } },
                { assertEquals(400, res.statusCode) }
            )
        }

        @Test
        fun `organization not found throws StatusCodeException 404`() {
            every { organizationRepository.findById(1) } returns Optional.empty()

            val res = assertThrows<StatusCodeException> {
                organizationInvitationService.createInvitation(
                    dummyUserBob, 1, dummyUserAlice.username, ScopeRole.ORG_MEMBER
                )
            }

            assertAll(
                { assertFalse { savedInvitationSlot.isCaptured } },
                { assertEquals(404, res.statusCode) }
            )
        }

        @Test
        fun `user not found throws StatusCodeException 404`() {
            every { userRepository.findByUsernameIgnoreCase("Unknown_user") } returns Optional.empty()
            val res = assertThrows<StatusCodeException> {
                organizationInvitationService.createInvitation(
                    dummyUserBob, 0, "Unknown_user", ScopeRole.ORG_MEMBER
                )
            }

            assertAll(
                { assertFalse { savedInvitationSlot.isCaptured } },
                { assertEquals(404, res.statusCode) }
            )
        }

        @Test
        fun `inviting with role ORG_OWNER throws StatusCodeException 400`() {
            val res = assertThrows<StatusCodeException> {
                organizationInvitationService.createInvitation(
                    dummyUserBob, 0, dummyUserAlice.username, ScopeRole.ORG_OWNER
                )
            }

            assertAll(
                { assertFalse { savedInvitationSlot.isCaptured } },
                { assertEquals(400, res.statusCode) }
            )
        }

        @Test
        fun `inviting organization member throws StatusCodeException 400`() {
            val res = assertThrows<StatusCodeException> {
                organizationInvitationService.createInvitation(
                    dummyUserBob, 0, dummyUserBob.username, ScopeRole.ORG_MEMBER
                )
            }

            assertAll(
                { assertFalse { savedInvitationSlot.isCaptured } },
                { assertEquals(400, res.statusCode) }
            )
        }

        @Test
        fun `happy path creates an organizationInvitation`() {
            organizationInvitationService.createInvitation(
                dummyUserBob, 0, dummyUserAlice.username, ScopeRole.ORG_MEMBER
            )

            assertTrue(savedInvitationSlot.isCaptured)
            val savedInvitation = savedInvitationSlot.captured

            assertAll(
                { assertEquals(0, savedInvitation.invitedBy.id) },
                { assertEquals(1, savedInvitation.invitedUser.id) },
                { assertEquals(42, savedInvitation.organization.id) },
                { assertEquals(ScopeRole.ORG_MEMBER, savedInvitation.role) }
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveInvitation() {
        private val savedUserScopeSlot: CapturingSlot<UserScope> = slot()
        private val savedInvitationSlot: CapturingSlot<OrganizationInvitation> = slot()

        @BeforeEach
        fun commonHappyStubs() {
            savedUserScopeSlot.clear()
            savedInvitationSlot.clear()

            every { userScopeRepository.save(capture(savedUserScopeSlot)) } returnsArgument 0
            every { organizationInvitationRepository.save(capture(savedInvitationSlot)) } returnsArgument 0
        }

        fun badInvitationStatusesProvider() = listOf(
            InvitationStatus.ACCEPTED, InvitationStatus.DECLINED
        )

        @Test
        fun `invitation not found throws StatusCodeException 404`() {
            every { organizationInvitationRepository.findById(-1) } returns Optional.empty()

            val res = assertThrows<StatusCodeException> {
                organizationInvitationService.resolveInvitation(
                    dummyUserAlice,
                    -1,
                    InvitationStatus.ACCEPTED
                )
            }

            assertAll(
                { assertFalse(savedInvitationSlot.isCaptured) },
                { assertFalse(savedUserScopeSlot.isCaptured) },
                { assertEquals(404, res.statusCode) }
            )
        }

        @Test
        fun `resolving someone else's invitation throws StatusCodeException 403`() {
            val res = assertThrows<StatusCodeException> {
                organizationInvitationService.resolveInvitation(
                    dummyUserBob,
                    0,
                    InvitationStatus.ACCEPTED
                )
            }

            assertAll(
                { assertFalse(savedInvitationSlot.isCaptured) },
                { assertFalse(savedUserScopeSlot.isCaptured) },
                { assertEquals(403, res.statusCode) }
            )
        }

        @ParameterizedTest
        @MethodSource("badInvitationStatusesProvider")
        fun `resolving answered invitation throws StatusCodeException 400`(currentInvitationStatus: InvitationStatus) {
            every { dummyInvitation.status } returns currentInvitationStatus

            val res = assertThrows<StatusCodeException> {
                organizationInvitationService.resolveInvitation(
                    dummyUserAlice,
                    0,
                    InvitationStatus.ACCEPTED
                )
            }

            assertAll(
                { assertFalse(savedInvitationSlot.isCaptured) },
                { assertFalse(savedUserScopeSlot.isCaptured) },
                { assertEquals(400, res.statusCode) }
            )
        }

        @Test
        fun `happy path DECLINED updates the invitation`() {
            organizationInvitationService.resolveInvitation(
                dummyUserAlice,
                0,
                InvitationStatus.DECLINED
            )

            assertAll(
                { assertFalse(savedUserScopeSlot.isCaptured) },
                { assertTrue(savedInvitationSlot.isCaptured) }
            )

            val savedInvitation = savedInvitationSlot.captured
            assertAll(
                { assertNotNull(savedInvitation.answeredAt) },
                { assertEquals(InvitationStatus.DECLINED, dummyInvitation.status) },
            )
        }

        @Test
        fun `happy path ACCEPTED updates the invitation and adds user to organization members`() {
            organizationInvitationService.resolveInvitation(
                dummyUserAlice,
                0,
                InvitationStatus.ACCEPTED
            )

            assertAll(
                { assertTrue(savedUserScopeSlot.isCaptured) },
                { assertTrue(savedInvitationSlot.isCaptured) }
            )

            val savedUserScope = savedUserScopeSlot.captured
            val savedInvitation = savedInvitationSlot.captured
            assertAll(
                { assertNotNull(savedInvitation.answeredAt) },
                { assertEquals(InvitationStatus.ACCEPTED, dummyInvitation.status) },
                { assertEquals(dummyOrganization.id, savedUserScope.scopeId) },
                { assertEquals(dummyUserAlice.id, savedUserScope.user.id) },
                { assertEquals(dummyInvitation.role, savedUserScope.role) },
            )
        }
    }
}
