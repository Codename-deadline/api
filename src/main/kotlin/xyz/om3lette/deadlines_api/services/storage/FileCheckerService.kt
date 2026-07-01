package xyz.om3lette.deadlines_api.services.storage

import org.apache.tika.Tika
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import xyz.om3lette.deadlines_api.data.attachments.enums.AttachmentCategory
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.util.requirePermission

data class AttachmentFileInfo(
    val mimeType: String,
    val category: AttachmentCategory,
    val sizeBytes: Long
)

@Service
class FileCheckerService(
    private val tika: Tika
) {
    private val forbiddenSubtypes: List<String> = listOf(
        "octet-stream", "x-csh", "java-archive", "vnd.apple.installer+xml", "x-sh"
    )

    private fun isFileAllowed(mimeType: String): Boolean {
        val (_, subtype) = mimeType.split("/")
        return !forbiddenSubtypes.contains(subtype)
    }

    fun getAttachmentFileInfoOr403(fileStream: MultipartFile): AttachmentFileInfo {
        val mimeType = TikaInputStream.get(fileStream.inputStream).use { inputStream ->
            tika.detect(inputStream, Metadata().apply {
                set(TikaCoreProperties.RESOURCE_NAME_KEY, fileStream.originalFilename)
            })
        }

        requirePermission(
            isFileAllowed(mimeType),
            { ErrorCode.ATTACHMENT_INVALID_FILE_TYPE to null },
            400
        )

        val attachmentCategory = when {
            mimeType.startsWith("video") -> AttachmentCategory.VIDEO
            mimeType.startsWith("audio") -> AttachmentCategory.AUDIO
            mimeType.startsWith("text") -> AttachmentCategory.TEXT
            mimeType.startsWith("image") -> AttachmentCategory.IMAGE
            else -> AttachmentCategory.OTHER
        }
        return AttachmentFileInfo(mimeType, attachmentCategory, fileStream.size)
    }
}
