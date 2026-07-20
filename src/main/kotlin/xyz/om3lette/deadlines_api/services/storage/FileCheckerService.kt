package xyz.om3lette.deadlines_api.services.storage

import org.apache.tika.Tika
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
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

    fun getAttachmentMimeTypeOr403(fileStream: MultipartFile): String {
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

        return mimeType
    }
}
