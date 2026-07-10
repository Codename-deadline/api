package xyz.om3lette.deadlines_api.data.notifications.enums

/**
 * Outbox notification status.
 *
 * - `SENT` - notification was successfully processed
 * - `IN_PROGRESS` - row is currently selected by a worker, but was not yet acted upon (locked)
 * - `PENDING` - waiting to be processed (not locked)
 * - `FAILED` - reached retry limit with no success
 *
 * Lifecycle:
 *
 * 1. Added to outbox -> `PENDING`
 * 2.  Selected by a worker for sending -> `IN_PROGRESS`
 * 3. Worker finished processing the notification:
 *   - if success -> `SENT`
 *   - if failed and retry limit not reached -> `PENDING`
 *   - if failed and retry limit reached -> `FAILED`
 */
enum class NotificationStatus(val code: String) {
    SENT("S"),
    IN_PROGRESS("I"),
    PENDING("P"),
    FAILED("F");

    companion object {
        fun fromCode(c: String) = NotificationStatus.entries.first { it.code == c }
    }
}