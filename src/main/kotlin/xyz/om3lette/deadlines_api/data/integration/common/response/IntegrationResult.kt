package xyz.om3lette.deadlines_api.data.integration.common.response

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.proto.GeneralResponse
import xyz.om3lette.deadlines_api.proto.Locale

data class IntegrationResult(
    val key: String,
    val language: Language = Language.EN
) {
    fun toResponse(): GeneralResponse = GeneralResponse.newBuilder()
        .setKey(key)
        .setLocale(Locale.valueOf(language.name))
        .build()
}