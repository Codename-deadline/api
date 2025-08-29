package xyz.om3lette.deadlines_api.data.notifications.converters

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import xyz.om3lette.deadlines_api.data.notifications.enums.NotificationStatus

@Converter(autoApply = true)
class NotificationStatusConverter : AttributeConverter<NotificationStatus, String> {
    override fun convertToDatabaseColumn(attribute: NotificationStatus?) = attribute?.code
    override fun convertToEntityAttribute(dbData: String?) = dbData?.let { NotificationStatus.fromCode(it) }
}
