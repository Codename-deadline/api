package xyz.om3lette.deadlines_api.data.attachments.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import xyz.om3lette.deadlines_api.data.attachments.constraints.AttachmentConstraints
import xyz.om3lette.deadlines_api.data.attachments.reponse.AttachmentPermissions
import xyz.om3lette.deadlines_api.data.attachments.reponse.AttachmentResponse
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.user.model.User
import java.time.Instant

@Entity
@Table(name = "deadline_attachments")
data class Attachment(
    @Id
    @SequenceGenerator(name = "ddl_attach_seq", sequenceName = "ddl_attach_sequence", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ddl_attach_seq")
    val id: Long,

    @Column(nullable = false, unique = true, length = 64)
    val objectKey: String,

    @field:NotBlank
    @field:Size(min = AttachmentConstraints.FILENAME_MIN, max = AttachmentConstraints.FILENAME_MAX)
    @Column(length = AttachmentConstraints.FILENAME_MAX, nullable = false)
    var filename: String,

    @Column(nullable = false)
    var mimeType: String,

    @Column(nullable = false)
    var sizeBytes: Long,

    @ManyToOne(fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id", nullable = true)
    val uploadedBy: User?,

    @ManyToOne(fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "deadline_id", nullable = false)
    val deadline: Deadline,

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    var uploadedAt: Instant
) {
    fun toResponse(permissions: AttachmentPermissions) = AttachmentResponse(
        id,
        filename,
        mimeType,
        sizeBytes,
        uploadedBy?.toMinimalResponse(),
        deadline.id,
        uploadedAt,
        permissions
    )
}
