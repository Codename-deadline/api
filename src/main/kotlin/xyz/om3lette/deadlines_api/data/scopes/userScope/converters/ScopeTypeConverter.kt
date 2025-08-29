package xyz.om3lette.deadlines_api.data.scopes.userScope.converters

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType

@Converter(autoApply = true)
class ScopeTypeConverter : AttributeConverter<ScopeType, String> {
    override fun convertToDatabaseColumn(attribute: ScopeType?) = attribute?.code
    override fun convertToEntityAttribute(dbData: String?)      = dbData?.let { ScopeType.fromCode(it) }
}
