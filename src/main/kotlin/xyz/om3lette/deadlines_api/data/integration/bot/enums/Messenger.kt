package xyz.om3lette.deadlines_api.data.integration.bot.enums

enum class Messenger {
    TELEGRAM;

    companion object {
        fun getByValue(value: Int) = entries.firstOrNull { it.ordinal == value }
    }
}