package xyz.om3lette.deadlines_api.data.common.constraints

enum class DatabaseConstraint(val databaseName: String) {
    PK_USERS("pk_users"),
    CK_USERS_USERNAME("ck_users_username"),
    CK_USERS_FULL_NAME("ck_users_full_name"),
    CK_USERS_LANGUAGE("ck_users_language"),
    CK_USERS_ROLE("ck_users_role"),
    UQ_USERS_USERNAME_LOWER("uq_users_username_lower"),

    PK_ORGANIZATIONS("pk_organizations"),
    CK_ORGANIZATIONS_TITLE("ck_organizations_title"),
    CK_ORGANIZATIONS_TYPE("ck_organizations_type"),

    PK_BOTS("pk_bots"),
    UQ_BOTS_MESSENGER("uq_bots_messenger"),
    CK_BOTS_MESSENGER("ck_bots_messenger"),

    PK_THREADS("pk_threads"),
    FK_THREADS_ORGANIZATION("fk_threads_organization"),
    CK_THREADS_TITLE("ck_threads_title"),

    PK_DEADLINES("pk_deadlines"),
    FK_DEADLINES_THREAD("fk_deadlines_thread"),
    CK_DEADLINES_TITLE("ck_deadlines_title"),

    PK_CHATS("pk_chats"),
    UQ_CHATS_MESSENGER_CHAT_ID_MESSENGER("uq_chats_messenger_chat_id_messenger"),
    FK_CHATS_BOT("fk_chats_bot"),
    CK_CHATS_MESSENGER("ck_chats_messenger"),
    CK_CHATS_LANGUAGE("ck_chats_language"),

    PK_REFRESH_TOKENS("pk_refresh_tokens"),
    UQ_REFRESH_TOKENS_JTI("uq_refresh_tokens_jti"),
    FK_REFRESH_TOKENS_USER("fk_refresh_tokens_user"),

    PK_ORGANIZATION_INVITATIONS("pk_organization_invitations"),
    CK_ORGANIZATION_INVITATIONS_DISTINCT_USERS("ck_organization_invitations_distinct_users"),
    FK_ORGANIZATION_INVITATIONS_INVITED_BY("fk_organization_invitations_invited_by"),
    FK_ORGANIZATION_INVITATIONS_INVITED_USER("fk_organization_invitations_invited_user"),
    FK_ORGANIZATION_INVITATIONS_ORGANIZATION("fk_organization_invitations_organization"),
    CK_ORGANIZATION_INVITATIONS_STATUS("ck_organization_invitations_status"),
    CK_ORGANIZATION_INVITATIONS_ROLE("ck_organization_invitations_role"),
    UQ_ORGANIZATION_INVITATIONS_PENDING("uq_organization_invitations_pending"),

    PK_USER_SCOPES("pk_user_scopes"),
    FK_USER_SCOPES_USER("fk_user_scopes_user"),
    CK_USER_SCOPES_SCOPE_TYPE("ck_user_scopes_scope_type"),
    CK_USER_SCOPES_ROLE_COMPATIBILITY("ck_user_scopes_role_compatibility"),
    UQ_USER_SCOPES_ORGANIZATION_OWNER("uq_user_scopes_organization_owner"),

    PK_USER_MESSENGER_ACCOUNTS("pk_user_messenger_accounts"),
    UQ_USER_MESSENGER_ACCOUNTS_ACCOUNT_MESSENGER("uq_user_messenger_accounts_account_messenger"),
    FK_USER_MESSENGER_ACCOUNTS_USER("fk_user_messenger_accounts_user"),
    CK_USER_MESSENGER_ACCOUNTS_MESSENGER("ck_user_messenger_accounts_messenger"),

    PK_CHAT_SUBSCRIPTIONS("pk_chat_subscriptions"),
    FK_CHAT_SUBSCRIPTIONS_CHAT("fk_chat_subscriptions_chat"),
    CK_CHAT_SUBSCRIPTIONS_SCOPE_TYPE("ck_chat_subscriptions_scope_type"),

    PK_DEADLINE_ATTACHMENTS("pk_deadline_attachments"),
    UQ_DEADLINE_ATTACHMENTS_OBJECT_KEY("uq_deadline_attachments_object_key"),
    FK_DEADLINE_ATTACHMENTS_USER("fk_deadline_attachments_user"),
    FK_DEADLINE_ATTACHMENTS_DEADLINE("fk_deadline_attachments_deadline"),
    CK_DEADLINE_ATTACHMENTS_FILENAME("ck_deadline_attachments_filename"),
    CK_DEADLINE_ATTACHMENTS_SIZE("ck_deadline_attachments_size"),

    PK_DEADLINE_NOTIFICATIONS("pk_deadline_notifications"),
    FK_DEADLINE_NOTIFICATIONS_DEADLINE("fk_deadline_notifications_deadline"),
    CK_DEADLINE_NOTIFICATIONS_TYPE("ck_deadline_notifications_type"),
    CK_DEADLINE_NOTIFICATIONS_STATUS("ck_deadline_notifications_status"),
    UQ_DEADLINE_NOTIFICATIONS_PENDING_TYPE("uq_deadline_notifications_pending_type"),

    PK_NOTIFICATION_OUTBOX("pk_notification_outbox"),
    FK_NOTIFICATION_OUTBOX_NOTIFICATION("fk_notification_outbox_notification"),
    CK_NOTIFICATION_OUTBOX_SOURCE("ck_notification_outbox_source"),
    CK_NOTIFICATION_OUTBOX_MESSENGER("ck_notification_outbox_messenger"),
    CK_NOTIFICATION_OUTBOX_STATUS("ck_notification_outbox_status"),
    CK_NOTIFICATION_OUTBOX_RETRIES("ck_notification_outbox_retries"),
}
