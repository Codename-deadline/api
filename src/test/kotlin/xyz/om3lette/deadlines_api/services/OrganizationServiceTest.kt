package xyz.om3lette.deadlines_api.services

import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationInvitationRole
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.InvitationDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationInvitation
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationInvitationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.user.enums.UserRole
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// TODO: Rewrite

@ExtendWith(MockKExtension::class)
class OrganizationServiceTest {
    private val userRepository: UserRepository = mockk()
    private val userScopeRepository: UserScopeRepository = mockk()
    private val threadRepository: ThreadRepository = mockk()
    private val deadlineRepository: DeadlineRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()
    private val organizationInvitationRepository: OrganizationInvitationRepository = mockk()
    private val permissionService: PermissionService = mockk()
    private val organizationInvitationService: OrganizationInvitationService = mockk()
    private val organizationService: OrganizationService = OrganizationService(
        userRepository,
        userScopeRepository,
        threadRepository,
        deadlineRepository,
        organizationRepository,
        organizationInvitationRepository,
        permissionService,
        organizationInvitationService
    )

    private val dummyUserBob = spyk(User(
        0,
        "Bob",
        Instant.now(),
        "Bob the tester",
        "raw-password"
    ))

    private val dummyUserAlice = spyk(User(
        1,
        "Alice",
        Instant.now(),
        "Alice the tester",
        "raw-password"
    ))

    private val dummyOrganization = Organization(
        256,
        "My first org",
        null,
        OrganizationType.PUBLIC,
        Instant.now(),
    )

    private val dummyInvitation = spyk(OrganizationInvitation(
        0,
        dummyUserBob,
        dummyUserAlice,
        dummyOrganization,
        InvitationStatus.PENDING,
        ScopeRole.ORG_ADMIN,
        Instant.now()
    ))

    private val dummyUserScopeBob = spyk(UserScope(
        512,
        dummyUserBob,
        ScopeType.ORGANIZATION,
        dummyOrganization.id,
        ScopeRole.ORG_OWNER,
        Instant.now()
    ))
    private val dummyUserScopeAlice = spyk(UserScope(
        128,
        dummyUserAlice,
        ScopeType.ORGANIZATION,
        dummyOrganization.id,
        ScopeRole.ORG_MEMBER,
        Instant.now()
    ))

    @BeforeEach
    fun commonHappyStubs() {
        clearMocks(userScopeRepository, recordedCalls = true)

        every { userScopeRepository.findByUserAndScopeId(dummyUserBob, dummyOrganization.id) } returns Optional.of(dummyUserScopeBob)
        every { userRepository.findByUsernameInIgnoreCase(emptyList()) } returns emptyList()
        every { userRepository.findByUsernameInIgnoreCase(listOf("alice")) } returns listOf(dummyUserAlice)
        every { userRepository.findByUsernameIgnoreCase("Alice") } returns Optional.of(dummyUserAlice)

        every { organizationRepository.findById(dummyOrganization.id) } returns Optional.of(dummyOrganization)

        every { userScopeRepository.deleteByUserAndScopeId(dummyUserAlice, dummyOrganization.id) } returns 1
        every { userScopeRepository.deleteByUserAndScopeIdIn(dummyUserAlice, any()) } returns 1
        every { threadRepository.findAllIdsByOrganizationId(any()) } returns listOf()
        every { deadlineRepository.findAllIdsByOrganizationId(any()) } returns listOf()

        every { dummyUserScopeBob.role } returns ScopeRole.ORG_OWNER

        dummyOrganization.members.clear()
        dummyOrganization.members.add(dummyUserScopeBob)
    }

    @Nested
    inner class CreateOrganization() {
        private val savedInvitationsSlot: CapturingSlot<List<OrganizationInvitation>> = slot()
        private val savedOrganizationSlot: CapturingSlot<Organization> = slot()
        private val savedUserScopeSlot: CapturingSlot<UserScope> = slot()

        @BeforeEach
        fun commonHappyStubs() {
            savedInvitationsSlot.clear()
            savedOrganizationSlot.clear()

            every { userScopeRepository.save(capture(savedUserScopeSlot)) } returnsArgument 0

            every { organizationRepository.save(capture(savedOrganizationSlot)) } returnsArgument 0

            every { organizationInvitationRepository.saveAll(capture(savedInvitationsSlot)) } returnsArgument 0
            every { organizationInvitationService.createInvitation(dummyUserBob, dummyUserAlice, any(), any()) } returns dummyInvitation
        }

        @Test
        fun `happy path no users to invite creates organization`() {
            organizationService.createOrganization(
                dummyUserBob,
                "My first org",
                null,
                OrganizationType.PUBLIC,
                listOf()
            )

            verify {
                organizationRepository.save(
                    match { it.title == "My first org" && it.description == null }
                )
            }

            assertAll(
                { assertTrue { savedOrganizationSlot.isCaptured } },
                { assertTrue { savedInvitationsSlot.isCaptured } },
                { assertTrue { savedUserScopeSlot.isCaptured } }
            )
            val savedInvitations = savedInvitationsSlot.captured
            val savedUserScope = savedUserScopeSlot.captured
            assertAll(
                { assertEquals(dummyUserBob.id, savedUserScope.user.id) },
                { assertEquals(ScopeRole.ORG_OWNER, savedUserScope.role) },
                { assertEquals(ScopeType.ORGANIZATION, savedUserScope.scopeType) },
                { assertEquals(0, savedInvitations.size)}
            )
        }

        @Test
        fun `happy path with a user to invite creates organization and an invitation`() {
            organizationService.createOrganization(
                dummyUserBob,
                "My first org",
                null,
                OrganizationType.PUBLIC,
                listOf(
                    InvitationDTO("Alice", OrganizationInvitationRole.ORG_ADMIN)
                )
            )

            assertAll(
                { assertTrue { savedOrganizationSlot.isCaptured } },
                { assertTrue { savedInvitationsSlot.isCaptured } },
                { assertTrue { savedUserScopeSlot.isCaptured } }
            )
            val savedInvitations = savedInvitationsSlot.captured
            val savedUserScope = savedUserScopeSlot.captured
            assertAll(
                { assertEquals(dummyUserBob.id, savedUserScope.user.id) },
                { assertEquals(ScopeRole.ORG_OWNER, savedUserScope.role) },
                { assertEquals(ScopeType.ORGANIZATION, savedUserScope.scopeType) },
                { assertEquals(1, savedInvitations.size)},
                { assertEquals(dummyUserBob.id, savedInvitations.first().invitedBy.id) },
                { assertEquals(dummyUserAlice.id, savedInvitations.first().invitedUser.id) },
                { assertEquals(ScopeRole.ORG_ADMIN, savedInvitations.first().role) },
                { assertEquals(dummyOrganization.id, savedInvitations.first().organization.id) },
            )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class DeleteOrganization() {

        private val deletedOrganizationSlot: CapturingSlot<Organization> = slot()

        @BeforeEach
        fun commonHappyStubs() {
            deletedOrganizationSlot.clear()

            every { organizationRepository.delete(capture(deletedOrganizationSlot)) } returnsArgument 0
            every { permissionService.canDeleteOrganization(any(), any()) } returns true
        }

        @Test
        fun `if organization not found throws StatusCodeException 404`() {
            val fakeOrganizationId = dummyOrganization.id + 1
            every { organizationRepository.findById(fakeOrganizationId) } returns Optional.empty()

            val res = assertThrows<StatusCodeException> {
                organizationService.deleteOrganization(dummyUserBob, fakeOrganizationId)
            }
            assertAll(
                { assertEquals(404, res.statusCode) },
                { assertFalse(deletedOrganizationSlot.isCaptured) }
            )
        }

        @Test
        fun `not enough permissions throws StatusCodeException 403`() {
            every { permissionService.canDeleteOrganization(any(), any()) } returns false

            val res = assertThrows<StatusCodeException> {
                organizationService.deleteOrganization(dummyUserBob, dummyOrganization.id)
            }
            assertAll(
                { assertEquals(403, res.statusCode) },
                { assertFalse(deletedOrganizationSlot.isCaptured) }
            )
        }

        @Test
        fun `happy path deletes the organization`() {
            val res = organizationService.deleteOrganization(dummyUserBob, dummyOrganization.id)

            verify {
                organizationRepository.delete(
                    match { it.id == dummyOrganization.id }
                )
            }
            assertTrue(deletedOrganizationSlot.isCaptured)
            val deletedOrganization = deletedOrganizationSlot.captured
            assertEquals(dummyOrganization.id, deletedOrganization.id)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class RemoveMember() {

        @BeforeEach
        fun commonHappyStubs() {
            every { permissionService.canManageOrganizationMembers(any(), any()) } returns true
        }

        @Test
        fun `if member is not found throws StatusCodeException 404`() {
            every { userRepository.findByUsernameIgnoreCase("UnknownMember") } returns Optional.empty()

            val res = assertThrows<StatusCodeException> {
                organizationService.removeMember(dummyUserBob, dummyOrganization.id, "UnknownMember")
            }

            assertAll(
                { verify(exactly = 0) {userScopeRepository.deleteByUserAndScopeIdIn(any(), any()) } },
                { assertEquals(404, res.statusCode) }
            )
        }

        @Test
        fun `not enough permissions throws StatusCodeException 403`() {
            every { permissionService.canManageOrganizationMembers(any(), any()) } returns false

            val res = assertThrows<StatusCodeException>{
                organizationService.removeMember(dummyUserBob, dummyOrganization.id, "Alice")
            }

            assertAll(
                { verify(exactly = 0) {userScopeRepository.deleteByUserAndScopeIdIn(any(), any()) } },
                { assertEquals(403, res.statusCode) }
            )
        }

        @Test
        fun `removing issuer throws StatusCodeException 400`() {
            val res = assertThrows<StatusCodeException>{
                organizationService.removeMember(dummyUserBob, dummyOrganization.id, "BOB")
            }

            assertAll(
                { verify(exactly = 0) { userScopeRepository.deleteByUserAndScopeIdIn(any(), any()) } },
                { assertEquals(400, res.statusCode) }
            )
        }

        @Test
        fun `happy path removes organization member`() {
            dummyOrganization.members.add(dummyUserScopeAlice)
            organizationService.removeMember(dummyUserBob, dummyOrganization.id, "Alice")

            verify(exactly = 1) { userScopeRepository.deleteByUserAndScopeId(dummyUserAlice, dummyOrganization.id) }
        }
    }
}