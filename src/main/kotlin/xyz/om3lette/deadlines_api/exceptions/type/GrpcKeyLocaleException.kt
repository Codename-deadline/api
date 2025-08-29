package xyz.om3lette.deadlines_api.exceptions.type

import io.grpc.Status
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.proto.Locale

data class GrpcKeyLocaleException(
    val status: Status,
    val key: String,
    val language: Language = Language.EN
) : RuntimeException(
    "Status: ${status.description}, key: $key"
) {
    val locale: Locale
        get() = Locale.valueOf(language.name)
}