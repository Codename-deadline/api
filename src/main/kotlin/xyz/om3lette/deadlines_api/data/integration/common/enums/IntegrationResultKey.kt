package xyz.om3lette.deadlines_api.data.integration.common.enums

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType

enum class IntegrationResultKey(private val template: String) {
    // ==========================================
    //                INFO MESSAGES
    // ==========================================
    ACCOUNT_LINKAGE_IGNORED("account_linkage.ignored"),
    ACCOUNT_LINKAGE_SUCCESS("account_linkage.success"),

    CHAT_INFO_UPDATE_SUCCESS("chat_info_update.success"),

    REGISTER_CHAT_SUCCESS("register_chat.success"),
    DEREGISTER_CHAT_SUCCESS("deregister_chat.success"),
    DEREGISTER_CHAT_NOT_REGISTERED("deregister_chat.not_registered"),

    SUBSCRIBE_ALREADY_SUBSCRIBED("sub.%s.already_subscribed"),
    SUBSCRIBE_SUCCESS("sub.%s.success"),
    UNSUBSCRIBE_ALL_SUCCESS("unsub.all.success"),
    UNSUBSCRIBE_NOT_SUBSCRIBED("unsub.%s.not_subscribed"),
    UNSUBSCRIBE_SUCCESS("unsub.%s.success"),

    // ==========================================
    //                   ERRORS
    // ==========================================
    LINKED_ACCOUNT_NOT_FOUND("errors.linked_account_not_found"),
    USER_NOT_FOUND("errors.user_not_found"),
    CHAT_REGISTRATION_DENIED("errors.chat_registration_denied"),

    CHAT_ALREADY_REGISTERED("errors.chat_already_registered"),
    CHAT_NOT_FOUND("errors.chat_not_found"),

    ORGANIZATION_ACCESS_DENIED("errors.organization_access_denied"),
    ORGANIZATION_NOT_FOUND("errors.organization_not_found"),

    THREAD_ACCESS_DENIED("errors.thread_access_denied"),
    THREAD_NOT_FOUND("errors.thread_not_found"),

    DEADLINE_ACCESS_DENIED("errors.deadline_access_denied"),
    DEADLINE_NOT_FOUND("errors.deadline_not_found"),

    REQUEST_NOT_FOUND("errors.request_not_found"),
    SERVER_INTERNAL("errors.server_internal"),
    ;


    fun value(scopeType: ScopeType? = null): String {
        if (!template.contains("%s")) return template
        require(scopeType != null) { "scopeType is required for $name" }
        return template.format(scopeType.name.lowercase())
    }
}