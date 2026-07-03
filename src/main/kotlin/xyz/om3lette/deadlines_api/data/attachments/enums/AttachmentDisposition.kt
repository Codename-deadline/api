package xyz.om3lette.deadlines_api.data.attachments.enums

enum class AttachmentDisposition(val headerValue: String) {
    ATTACHMENT("attachment"),
    INLINE("inline");

    companion object {
        fun from(value: String?): AttachmentDisposition = when (value?.lowercase()) {
            null, "", "attachment" -> ATTACHMENT
            "inline" -> INLINE
            else -> ATTACHMENT
        }
    }
}
