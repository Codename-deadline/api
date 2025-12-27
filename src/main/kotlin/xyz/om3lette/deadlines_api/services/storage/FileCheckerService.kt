package xyz.om3lette.deadlines_api.services.storage

import org.apache.tika.Tika
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import xyz.om3lette.deadlines_api.data.attachments.enums.AttachmentType
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.util.requirePermission

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

    fun getAttachmentTypeOr403(fileStream: MultipartFile): Pair<String, AttachmentType> {
        val mimeType = tika.detect(TikaInputStream.get(fileStream.inputStream), Metadata().apply {
            set(TikaCoreProperties.RESOURCE_NAME_KEY, fileStream.originalFilename)
        })

        requirePermission(
            isFileAllowed(mimeType),
            { ErrorCode.ATTACHMENT_INVALID_FILE_TYPE to null },
            400
        )

        val attachmentType = when {
            mimeType.startsWith("video") -> AttachmentType.VIDEO
            mimeType.startsWith("audio") -> AttachmentType.AUDIO
            mimeType.startsWith("text") -> AttachmentType.TEXT
            mimeType.startsWith("image") -> AttachmentType.IMAGE
            else -> AttachmentType.OTHER
        }
        return Pair(mimeType, attachmentType)
    }
}