package xyz.om3lette.deadlines_api.data.notifications.enums

enum class NotificationStatus(val code: String) {
    SENT("S"),
    IN_PROGRESS("I"),
    PENDING("P"),
    FAILED("F");

    companion object {
        fun fromCode(c: String) = NotificationStatus.entries.first { it.code == c }
    }
}