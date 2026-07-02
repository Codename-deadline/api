package xyz.om3lette.deadlines_api.data.attachments.model

import jakarta.persistence.*
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.attachments.enums.AttachmentCategory
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

    val objectKey: String,

    @field:Size(min = 1, max = 64)
    var filename: String,

    @Enumerated(value = EnumType.STRING)
    var category: AttachmentCategory,

    var mimeType: String,

    var sizeBytes: Long,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    val uploadedBy: User,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deadline_id")
    val deadline: Deadline,

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    var uploadedAt: Instant
) {
    fun toMap() = mapOf(
        "id" to id,
        "filename" to filename,
        "category" to category.name,
        "mimeType" to (mimeType),
        "sizeBytes" to (sizeBytes),
        "uploadedBy" to uploadedBy.toMap(),
        "attachedTo" to deadline.id,
        "uploadedAt" to uploadedAt
    )

    fun toResponse() = AttachmentResponse(
        id,
        filename,
        category.name,
        mimeType,
        sizeBytes,
        uploadedBy.toMinimalResponse(),
        deadline.id,
        uploadedAt
    )
}
