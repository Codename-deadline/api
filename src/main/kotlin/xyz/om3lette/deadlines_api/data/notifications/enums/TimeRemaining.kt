package xyz.om3lette.deadlines_api.data.notifications.enums

import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
enum class TimeRemaining {
    FIFTEEN_MINUTES,
    ONE_HOUR,
    ONE_DAY,
    ONE_WEEK,
    ONE_MONTH;
}
