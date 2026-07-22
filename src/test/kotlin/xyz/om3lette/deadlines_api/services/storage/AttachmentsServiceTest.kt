package xyz.om3lette.deadlines_api.services.storage

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.configs.properties.AttachmentsProperties
import xyz.om3lette.deadlines_api.configs.properties.StorageProperties
import xyz.om3lette.deadlines_api.data.attachments.model.Attachment
import xyz.om3lette.deadlines_api.data.attachments.repo.AttachmentRepository
import xyz.om3lette.deadlines_api.data.attachments.reponse.AttachmentPermissions
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals

class AttachmentsServiceTest {
    private val permissionService: PermissionService = mockk()
    private val attachmentRepository: AttachmentRepository = mockk()
    private val deadlineRepository: DeadlineRepository = mockk()
    private val attachmentsService = AttachmentsService(
        s3Client = mockk<S3Client>(),
        s3Presigner = mockk<S3Presigner>(),
        storageProperties = StorageProperties(StorageProperties.S3(endpoint = "http://storage")),
        attachmentsProperties = AttachmentsProperties(),
        permissionService = permissionService,
        attachmentRepository = attachmentRepository,
        deadlineRepository = deadlineRepository,
        fileCheckerService = mockk<FileCheckerService>()
    )

    private lateinit var deadline: Deadline
    private lateinit var attachment: Attachment

    @BeforeEach
    fun commonHappyStubs() {
        val organization = DomainObjectBuilder.organization()
        val thread = DomainObjectBuilder.thread(organization)
        deadline = DomainObjectBuilder.deadline(thread)
        attachment = Attachment(
            id = 1,
            objectKey = "attachment-key",
            filename = "attachment.txt",
            mimeType = "text/plain",
            sizeBytes = 1,
            uploadedBy = null,
            deadline = deadline,
            uploadedAt = Instant.EPOCH
        )

        every { attachmentRepository.findById(attachment.id) } returns Optional.of(attachment)
        every { deadlineRepository.findById(deadline.id) } returns Optional.of(deadline)
        every { permissionService.hasAccess(null, DeadlineScope(deadline)) } returns true
        every {
            permissionService.buildDeadlineAttachmentPermissions(null, attachment)
        } returns AttachmentPermissions(update = false, delete = false)
    }

    @Test
    fun `anonymous attachment metadata uses deadline access check`() {
        val result = attachmentsService.getAttachmentMetadata(null, attachment.id)

        assertEquals(AttachmentPermissions(update = false, delete = false), result.permissions)
        verify(exactly = 1) { permissionService.hasAccess(null, DeadlineScope(deadline)) }
    }

    @Test
    fun `anonymous deadline attachments use deadline access check`() {
        every {
            attachmentRepository.findAllByDeadlineOrderByUploadedAtDesc(deadline)
        } returns listOf(attachment)

        val result = attachmentsService.getDeadlineAttachmentsMetadata(null, deadline.id)

        assertEquals(AttachmentPermissions(update = false, delete = false), result.single().permissions)
        verify(exactly = 1) { permissionService.hasAccess(null, DeadlineScope(deadline)) }
    }

    @Test
    fun `anonymous attachment metadata is rejected when deadline access is denied`() {
        every { permissionService.hasAccess(null, DeadlineScope(deadline)) } returns false

        val error = assertThrows<StatusCodeException> {
            attachmentsService.getAttachmentMetadata(null, attachment.id)
        }

        assertEquals(403, error.statusCode)
        verify(exactly = 0) { permissionService.buildDeadlineAttachmentPermissions(any(), any()) }
    }
}
