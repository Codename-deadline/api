package xyz.om3lette.deadlines_api.exceptions.enums

enum class ErrorCode(val code: String) {
    UNKNOWN_ERROR("unknown-error"),
    DESERIALIZATION_ERROR("deserialization-error"),

    AUTH_INSUFFICIENT_PERMISSIONS("auth.insufficient-permissions"),
    AUTH_INVALID_CREDENTIALS("auth.invalid-credentials"),
    AUTH_SESSIONS_LIMIT_EXCEEDED("auth.sessions-limit-exceeded"),

    SIGN_UP_REGISTRATION_REQUEST_NOT_FOUND("sign-up.request-not-found"),
    SIGN_UP_INSUFFICIENT_DATA("sign-up.insufficient-data"),

    PASSWORD_CHANGE_UNCHANGED("password-change.unchanged"),
    PASSWORD_CHANGE_INVALID_CREDENTIALS("password-change.invalid-credentials"),

    USER_ALREADY_EXISTS("user.already-exists"),
    USER_NOT_FOUND("user.not-found"),

    ACTION_SELF_REMOVAL("action.self-removal"),

    ROLE_CHANGE_SELF("role-change.self"),
    ROLE_CHANGE_INVALID_SCOPE_ROLE("role-change.invalid-scope-role"),
    ROLE_CHANGE_NO_ROLE("role-change.no-role"),

    INVITATION_NOT_FOUND("invitation.not-found"),
    INVITATION_INVALID_ROLE("invitation.invalid-role"),
    INVITATION_PERSONAL_ORG("invitation.personal-org"),
    INVITATION_ALREADY_ORG_MEMBER("invitation.already-org-member"),
    INVITATION_ALREADY_ANSWERED("invitation.already-answered"),

    ORG_NOT_FOUND("organization.not-found"),

    THR_NOT_FOUND("thread.not-found"),

    DDL_NOT_FOUND("deadline.not-found"),
    DDL_INVALID_TIMESTAMP("deadline.invalid-timestamp"),

    INTEGRATION_ACCOUNT_ALREADY_IN_USE("integration.account-not-available"),
    INTEGRATION_ACCOUNT_NOT_LINKED("integration.account-not-linked"),
    INTEGRATION_INVALID_IDENTIFIER_FORMAT("integration.invalid-identifier-format"),
    INTEGRATION_MESSENGER_LINKAGE_LIMIT_EXCEEDED("integration.messenger-linkage-limit-exceeded"),

    ATTACHMENT_NOT_FOUND("attachment.not-found"),
    ATTACHMENT_UPLOAD_FAILED("attachment.upload-failed"),
    ATTACHMENT_INVALID_FILE_TYPE("attachment.invalid-file-type")
}
