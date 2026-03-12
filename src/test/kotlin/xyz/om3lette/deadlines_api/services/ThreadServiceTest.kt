package xyz.om3lette.deadlines_api.services

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.response.ThreadResponse
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class ThreadServiceTest {
    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var userScopeRepository: UserScopeRepository

    @MockK
    lateinit var threadRepository: ThreadRepository

    @MockK
    lateinit var organizationRepository: OrganizationRepository

    @MockK
    lateinit var permissionService: PermissionService

    @InjectMockKs
    lateinit var threadService: ThreadService

    private lateinit var dummyUserBob: User

    private lateinit var dummyUserAlice: User
    private val dummyUserScopeAlice: UserScope = spyk()

    private lateinit var organization: Organization
    private lateinit var thread: Thread

    fun orgScope() = OrganizationScope(organization.id, organization)
    fun thrScope() = ThreadScope(thread)

    private val savedThreadSlot: CapturingSlot<Thread> = slot()

    @BeforeEach
    fun commonHappyStubs() {
        dummyUserAlice = DomainObjectBuilder.userAlice()
        dummyUserBob = DomainObjectBuilder.userBob()

        organization = DomainObjectBuilder.organization()
        thread = DomainObjectBuilder.thread(organization)

        every { organizationRepository.findById(organization.id) } returns Optional.of(organization)
        every { threadRepository.findById(thread.id) } returns Optional.of(thread)

        every { dummyUserScopeAlice.user } returns dummyUserAlice
        every { dummyUserScopeAlice.role } returns ScopeRole.ORG_MEMBER

        every { permissionService.hasAccess(dummyUserBob, orgScope()) } returns true
        every { permissionService.hasAccess(dummyUserBob, thrScope()) } returns true
    }

    @Nested
    inner class CreateThread {
        private val savedUserScopesSlot: CapturingSlot<List<UserScope>> = slot()

        @BeforeEach
        fun commonHappyStubs() {
            every { threadRepository.save(capture(savedThreadSlot)) } returnsArgument 0

            every { userScopeRepository.saveAll(capture(savedUserScopesSlot)) } returns listOf()
            every {
                userScopeRepository.findByScopeIdAndScopeTypeUsernameInIgnoreCase(
                    organization.id, ScopeType.ORGANIZATION, emptyList()
                )
            } returns emptyList()
            every {
                userScopeRepository.findByScopeIdAndScopeTypeUsernameInIgnoreCase(
                    organization.id, ScopeType.ORGANIZATION, listOf(dummyUserAlice.username)
                )
            } returns listOf(dummyUserScopeAlice)

            every { permissionService.canCreateThread(any(), organization.id) } returns true
        }

        @Test
        fun `permissions not satisfied throws StatusCodeException 403`() {
            every { permissionService.canCreateThread(any(), organization.id) } returns false

            val res = assertThrows<StatusCodeException> {
                threadService.createThread(
                    dummyUserBob,organization.id, "t", null, listOf()
                )
            }
            assertEquals(403, res.statusCode)
            assertFalse(savedThreadSlot.isCaptured)
        }

        @Test
        fun `organization not found throws StatusCodeException 404`() {
            every { organizationRepository.findById(organization.id) } returns Optional.empty()

            val res = assertThrows<StatusCodeException> {
                threadService.createThread(
                    dummyUserBob,organization.id, "t", null, listOf()
                )
            }
            assertEquals(404, res.statusCode)
            assertFalse(savedThreadSlot.isCaptured)
        }

        @Test
        fun `happy path (no assignees) commits thread and returns threadId`() {
            val res =
                threadService.createThread(
                    dummyUserBob,organization.id, "t", null, listOf()
                )
            assertTrue(savedThreadSlot.isCaptured)
            assertEquals(0, savedUserScopesSlot.captured.size)
            assertEquals(savedThreadSlot.captured.id, res.threadId)
        }

        @Test
        fun `happy path commits thread, creates UserScopes for assignees and returns threadId`() {
            val res =
                threadService.createThread(
                    dummyUserBob,organization.id, "t", null, listOf(
                        dummyUserAlice.username
                    )
                )
            assertTrue(savedThreadSlot.isCaptured)
            assertTrue(savedUserScopesSlot.isCaptured)
            assertEquals(savedThreadSlot.captured.id, res.threadId)
            assertAll(
                { assertEquals(1, savedUserScopesSlot.captured.size) },
                { assertEquals(dummyUserAlice.username, savedUserScopesSlot.captured.first().user.username) },
                { assertEquals(ScopeRole.THR_ASSIGNEE, savedUserScopesSlot.captured.first().role) }
            )
        }
    }

    @Nested
    inner class DeleteThread {
        @BeforeEach
        fun commonHappyStubs() {
            every { threadRepository.delete(capture(savedThreadSlot)) } returnsArgument 0
            every { permissionService.canDelete(any(), any()) } returns true
        }

        @Test
        fun `permissions not satisfied throws StatusCodeException 403`() {
            every { permissionService.canDelete(dummyUserBob, any()) } returns false

            val res = assertThrows<StatusCodeException> {
                threadService.deleteThread(dummyUserBob,thread.id)
            }
            assertEquals(403, res.statusCode)
            assertFalse(savedThreadSlot.isCaptured)
        }

        @Test
        fun `happy path deletes the thread`() {
            threadService.deleteThread(dummyUserBob,thread.id)
            assertTrue(savedThreadSlot.isCaptured)
            assertEquals(thread.id, savedThreadSlot.captured.id)
        }
    }

    @Nested
    inner class RemoveAssignee() {
        private val deletedUserSlot: CapturingSlot<User> = slot()

        @BeforeEach
        fun commonHappyStubs() {
            val aliceUsername = dummyUserAlice.username
            every { userRepository.findByUsernameIgnoreCase(aliceUsername) } returns Optional.of(dummyUserAlice)
            every { permissionService.canManageAssignees(any(), thrScope()) } returns true

            every { userScopeRepository.deleteByUserAndScopeId(
                capture(deletedUserSlot), any(), thread.id, any()
            ) } returns 0
        }

        @Test
        fun `removing the issuer throws StatusCodeException 400`() {
            val res = assertThrows<StatusCodeException> {
                threadService.removeAssignee(dummyUserBob,thread.id, dummyUserBob.username)
            }
            assertEquals(400, res.statusCode)
            assertFalse(deletedUserSlot.isCaptured)
        }

        @Test
        fun `permissions not satisfied throws StatusCodeException 403`() {
            every { permissionService.canManageAssignees(any(), thrScope()) } returns false

            val res = assertThrows<StatusCodeException> {
                threadService.removeAssignee(dummyUserBob,thread.id, dummyUserAlice.username)
            }
            assertEquals(403, res.statusCode)
            assertFalse(deletedUserSlot.isCaptured)
        }

        @Test
        fun `user to remove not found throws StatusCodeException 404`() {
            every { userRepository.findByUsernameIgnoreCase(any()) } returns Optional.empty()

            val res = assertThrows<StatusCodeException> {
                threadService.removeAssignee(dummyUserBob,thread.id, dummyUserAlice.username)
            }
            assertEquals(404, res.statusCode)
            assertFalse(deletedUserSlot.isCaptured)
        }

        @Test
        fun `happy path deletes UserScope`() {
            threadService.removeAssignee(dummyUserBob,thread.id, dummyUserAlice.username)

            assertTrue(deletedUserSlot.isCaptured)
            assertEquals(dummyUserAlice.username, deletedUserSlot.captured.username)
        }
    }

    @Nested
    inner class GetThreadMetaData {
        @Test
        fun `permissions not satisfied throws StatusCodeException 403`() {
            every { permissionService.hasAccess(dummyUserBob, any()) } returns false

            val res = assertThrows<StatusCodeException> {
                threadService.getThreadMetaData(dummyUserBob,thread.id)
            }
            assertEquals(403, res.statusCode)
        }

        @Test
        fun `happy path returns thread map`() {
            every { permissionService.canManageAssignees(dummyUserBob, any()) } returns false

            val res: ThreadResponse = threadService.getThreadMetaData(dummyUserBob,thread.id)
            assertAll(
                { assertEquals(thread.id, res.id) },
                { assertEquals(thread.organization.id, res.organizationId) },
                { assertEquals(thread.title, res.title) },
                { assertEquals(thread.description, res.description) }
            )
        }
    }

    @Nested
    inner class GetThreadsByOrganization {
        @BeforeEach
        fun commonHappyStubs() {
            every { permissionService.hasAccess(dummyUserBob, orgScope())} returns true
            every { threadRepository.findAllByOrganization(organization, any()) } returns PageImpl(emptyList())
        }

        @Test
        fun `permissions not satisfied throws StatusCodeException 403`() {
            every { permissionService.hasAccess(dummyUserBob, orgScope())} returns false

            val res = assertThrows<StatusCodeException> {
                threadService.getThreadsByOrganization(
                    dummyUserBob,
                    organization.id,
                    0,
                    5
                )
            }
            verify(exactly = 0) { threadRepository.findAllByOrganization(organization, any()) }
            assertEquals(403, res.statusCode)
        }

        @Test
        fun `happy path calls threadRepository findAllByOrganization`() {
            threadService.getThreadsByOrganization(
                dummyUserBob,
                organization.id,
                0,
                5
            )
            verify(exactly = 1) { threadRepository.findAllByOrganization(organization, any()) }
        }
    }

    @Nested
    inner class PathThread() {
        private val savedThreadSlot: CapturingSlot<Thread> = slot()

        @BeforeEach
        fun commonHappyStubs() {
            every { permissionService.canUpdate(dummyUserBob, thrScope())} returns true
            every { threadRepository.save(capture(savedThreadSlot)) } returnsArgument 0
        }

        @Test
        fun `permissions not satisfied throws StatusCodeException 403`() {
            every { permissionService.canUpdate(dummyUserBob, thrScope())} returns false

            val res = assertThrows<StatusCodeException> {
                threadService.patchThread(dummyUserBob, thread.id, "new-t", "new-d")
            }
            assertFalse(savedThreadSlot.isCaptured)
            assertEquals(403, res.statusCode)
        }

        @Test
        fun `happy path commits thread`() {
            val newTitle = "new-title"
            val newDescription = "new-description"

            threadService.patchThread(dummyUserBob, thread.id, newTitle, newDescription)

            assertTrue(savedThreadSlot.isCaptured)
            assertAll(
                { assertEquals(newTitle, savedThreadSlot.captured.title) },
                { assertEquals(newDescription, savedThreadSlot.captured.description) }
            )
        }
    }

    @Nested
    inner class GetThreadAssignees {
        @BeforeEach
        fun commonHappyStubs() {
            val threadId = thread.id
            every { userScopeRepository.findAllByScopeIdAndScopeType(
                threadId, ScopeType.THREAD, any()
            ) } returns PageImpl(emptyList())
        }

        @Test
        fun `permissions not satisfied throws StatusCodeException 403`() {
            every { permissionService.hasAccess(dummyUserBob, thrScope())} returns false

            val res = assertThrows<StatusCodeException> {
                threadService.getThreadAssignees(dummyUserBob, thread.id, 0, 5)
            }

            verify(exactly = 0) { userScopeRepository.findAllByScopeIdAndScopeType(
                any(), any(), any()
            ) }
            assertEquals(403, res.statusCode)
        }

        @Test
        fun `happy path returns thread assignees`() {
            threadService.getThreadAssignees(dummyUserBob, thread.id, 0, 5)

            verify(exactly = 1) { userScopeRepository.findAllByScopeIdAndScopeType(
                any(), any(), any()
            ) }
        }
    }
}